# pcaputils.py, v0.1.0
#
# Jeffrey J. Guy, jjg@case.edu 
# 17 Dec 08
#


import os
import sys
import struct
import socket
import string

def __printable__(x):
    if ord(x) > 32 and ord(x) < 126:
        return chr(ord(x))
    else:
        return '.'

def hexdump(s):
    """ returns a string with hexdump -C style output of a bytestream """
    bytes = ["%.2x" % ord(x) for x in s]
    chars = [__printable__(x) for x in s]
    outstr = ""
    for i in xrange(0, len(bytes)/16):
        outstr += '    %s' % string.join(bytes[i*16:(i+1)*16],' ')
        outstr += ' ' 
        outstr += '%s\n' % string.join(chars[i*16:(i+1)*16], '')
    outstr += '    %s' % string.join(bytes[(i+1)*16:],' ')
    outstr += ' '
    outstr += '%s' % string.join(chars[(i+1)*16:],'')

    return outstr

def decodeEtherHeader(s):
    """ Takes in a raw byte string and returns a dictionary
    with the following keys:

    destMAC
    srcMAC
    etherType
    data
    """
    d = {}
    d['destMAC'] = s[:6]
    d['srcMAC'] = s[6:12]
    d['etherType'] = struct.unpack(">H", s[12:14])[0]
    d['data'] = s[14:]

    return d

def decodeUDPHeader(s):
    """ Takes in a raw byte string of an IP packet payload
    and returns a dictionary with the following keys:

    srcPort
    dstPort
    len
    checksum
    data
    """
    d = {}
    d['srcPort'] = struct.unpack(">H", s[:2])[0]
    d['dstPort'] = struct.unpack(">H", s[2:4])[0]
    d['len'] =     struct.unpack(">H", s[4:6])[0]
    d['checksum'] =struct.unpack('>H', s[6:8])[0]
    d['data'] = s[8:]
    return d

def decodeTCPHeader(s):
    """ Takes in a raw byte string of an IP packet payload
    and returns a dictionary with the following keys:

    srcPort
    dstPort
    seqNum
    sckNum
    dataOffset
    flags
    window
    checksum
    urg
    options [MAYBE]
    data
    """
    d = {}
    d['srcPort'] = struct.unpack(">H", s[:2])[0]
    d['dstPort'] = struct.unpack(">H", s[2:4])[0]
    d['seqNum'] = struct.unpack(">I", s[4:8])[0] 
    d['ackNum'] = struct.unpack(">I", s[8:12])[0]
    d['dataOffset'] = struct.unpack("B", s[12])[0] >> 2
    d['flags'] = struct.unpack("B", s[13])[0] & 0x3F
    d['window'] = struct.unpack(">H", s[14:16])[0]
    d['checksum'] = struct.unpack(">H", s[16:18])[0]
    d['urg'] = struct.unpack(">H", s[18:20])[0]
    if d['dataOffset'] > 5:
        d['options'] = s[20:4*(d['dataOffset'] - 5)]
    else:
        d['options'] = None
        d['data'] = s[20:]
    return d

def decodeIPHeader(s):
    """ Takes in a raw byte string of an IP packet
    and returns a dictionary with the following keys:
    
    version
    header_len
    tos
    total_len
    id
    flags
    fragment_offset
    ttl
    protocol
    checksum
    srcAddr
    dstAddr 
    options [MAYBE]
    data
    """
    # from pylibpcap... 
    d = {}
    d['version'] =    (ord(s[0]) & 0xf0) >> 4
    d['header_len'] = ord(s[0]) & 0x0f
    d['tos'] =        ord(s[1])
    d['total_len'] =  socket.ntohs(struct.unpack('H',s[2:4])[0])
    d['id'] =         socket.ntohs(struct.unpack('H',s[4:6])[0])
    d['flags'] =      (ord(s[6]) & 0xe0) >> 5
    d['fragment_offset'] = socket.ntohs(struct.unpack('H',s[6:8])[0] & 0x1f)
    d['ttl'] =        ord(s[8])
    d['protocol'] =   ord(s[9])
    d['checksum'] =   socket.ntohs(struct.unpack('H',s[10:12])[0])
    d['srcAddr'] =    socket.inet_ntoa(s[12:16])
    d['dstAddr'] =    socket.inet_ntoa(s[16:20])
    if d['header_len'] > 5:
        d['options'] = s[20:4*(d['header_len']-5)]
    else:
        d['options'] = None
        d['data'] = s[4*d['header_len']:]
    return d

class pcaputils:
    """ pcaputils(path) will open the capture file at path and 
        allow packet parsing with next() or auto() """
    def __init__(self, path):
        self.endian = None
        self.f = None
        self.path = None
        self.verMaj = None
        self.verMin = None
        self.tZone = None
        self.accuracy = None
        self.snaplen = None
        self.netType = None

        self.gHeaderFmt = "%sHHiIII"
        self.gHeaderLen = 20
        self.pHeaderFmt = "%sIIII"
        self.pHeaderLen = 16

        self.handlers = []

        if path: self.__open__(path)

    def __open__(self, path):
        self.path = path
        self.f = file(path, "r")

        magicNumber = self.f.read(4)
        if magicNumber == "\xa1\xb2\xc3\xd4":
           self.endian = ">"
        elif magicNumber == "\xd4\xc3\xb2\xa1":
           self.endian = "<"
        else:
           raise Exception("unknown magic number: %s" % repr(magicNumber))
        self.gHeaderFmt = self.gHeaderFmt % self.endian
        self.pHeaderFmt = self.pHeaderFmt % self.endian

        globalHeader = self.f.read(self.gHeaderLen)
        self.verMaj, self.verMin, self.tZone, \
        self.accuracy, self.snaplen, self.netType = struct.unpack(self.gHeaderFmt, globalHeader)
    
        return True
    
    def next(self):
        """ trim the next packet off the capture and return it 
            returns a tuple: (timestampSeconds, timestampUSeconds, packetData) """

        # TODO: return pythonic time format, incl timezones.

        packetHeader = self.f.read(self.pHeaderLen)
        while packetHeader:
            tsSec, tsUSec, capLen, realLen = struct.unpack(self.pHeaderFmt, packetHeader)
            packetData = self.f.read(capLen)
            yield (tsSec, tsUSec, packetData)
            packetHeader = self.f.read(self.pHeaderLen)
    
    def subscribe(self, expr, fn):
        """ subscribe handler fn to packets matching expr """

        code = compile(expr, "[pcap subscriptions]", "eval")
        self.handlers.append((expr, fn)) 

    def auto(self):
        """ will loop through the packet capture, parse the ethernet, 
            ip, tcp and/or udp headers.
            
            will compare the current packet to each subscription, 
            and call the handler function on a match.

            handler functions must take four arguments:
               handler(timestampSeconds, timestampUSeconds, headerDict, fullPacketData)
 
            subscriptions match on header values, using 
            dictionaries named 'ether', 'ip', 'tcp' and/or 'udp' 
            and the key values from the pcaputils.decode* functions. 
        """

        packetHeader = self.f.read(self.pHeaderLen)
        while packetHeader:
            ip = None
            ether = None
            udp = None
            tcp = None

            tsSec, tsUSec, capLen, realLen = struct.unpack(self.pHeaderFmt, packetHeader)
            packetData = self.f.read(capLen)
            
            ether = decodeEtherHeader(packetData)
            if ether['etherType'] == 0x0800:
                ip = decodeIPHeader(ether['data'])

                if ip['protocol'] == socket.IPPROTO_UDP:
                    udp= decodeUDPHeader(ip['data'])

                elif ip['protocol'] == socket.IPPROTO_TCP:
                    tcp = decodeTCPHeader(ip['data'])
            

            globals = {'ether': ether, 'ip': ip, 'tcp': tcp, 'udp': udp}
            for code,fn in self.handlers:
                try:  result = eval(code, globals)
                except TypeError: result = False
                if result:
                    fn(tsSec, tsUSec, packetData, globals)
 
            packetHeader = self.f.read(self.pHeaderLen)
