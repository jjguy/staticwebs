#
# based heavily on implementation of hopfield network
# found at http://www.ip-atlas.com/pub/nap/nn-src/
#
from PIL import PngImagePlugin
import sys
import glob
import os
import random

class Net:
    units = 0
    output = []
    threshold = []
    weight = []

INPUTS = None
NUM_DATA = None
N = None
X = None
Y = None

def allocate(net, path):
    """ imports the png images, returns vals about
        the data set.  then allocates space for our
        network based on the dataset"""

    n, inputs, num_data, x, y = getFiles(path)

    # "allocate" placeholders for later data
    net.weight = [[] for a in range(n)]
    for w in range(len(net.weight)):
        net.weight[w] = [0 for a in range(n)]
    net.output = [None for a in range(n)]
    net.threshold = [0 for a in range(n)]
    
    return (n, inputs, num_data, x, y)

def getFiles(path):
    """ takes path in, opens all pngs in the dir
        and imports those into our input array
        returns size of matrix, the input matrices, 
        the number of inputs and x, y dims """
        
    x = y = None
    files = glob.glob(os.path.join(path, "*.png"))
    inputs = []
    for f in files:
        print "Importing %s" % f
        img = PngImagePlugin.PngImageFile(file(f, "rb"))
        if not img: raise Exception("error parsing %s as png" % f)

        x,y = img.size
        inputs.append([None for a in range(x*y)])
 
        for i in range(x):
            for j in range(y):
                pix = img.getpixel((i, j))

                if pix == 0x00: val = 1
                else: val = -1

                inputs[-1][i*x+j] = val

    return (x*y, inputs, len(inputs), x, y) 

def calcWeights(net):
    for i in range(N):
        for j in range(N):
            weight = 0
            if i == j: continue
            for n in range(NUM_DATA):
                weight += INPUTS[n][i] * INPUTS[n][j]
            net.weight[i][j] = weight

def SetInput(net, data):
    for i in range(N):
        net.output[i] = data[i]
    WriteNet(net)

def GetOutput(net, data):
    for i in range(N):
        data[i] = net.output[i]
    WriteNet(net)

def WriteNet(net):
    for x in range(X):
        for y in range(Y):
            if net.output[x*X+y] == 1: ch = 'X'
            else: ch = ' '
            sys.stdout.write("%s" % ch)
        print
    print
 
def PropogateUnit(net, i):
    changed = False
    sum = 0
    out = None
    for j in range(N):
        sum += net.weight[i][j] * net.output[j]

    if sum != net.threshold[i]:
        if sum < net.threshold[i]: out = -1
        if sum > net.threshold[i]: out = 1
        if out != net.output[i]:
            changed = True
            net.output[i] = out
    return changed

def PropogateNet(net):
    iteration = 0
    iterationOfLastChange = 0

    while True:
        iteration += 1
        if PropogateUnit(net, random.randint(0, N-1)):
            iterationOfLastChange = iteration  
        if (iteration - iterationOfLastChange) > (10 * N): break
    print "%d iterations" % iteration
 
def SimulateNet(net, data):
    output = [None for x in range(N)]
    SetInput(net, data)
    PropogateNet(net)
    GetOutput(net, output)

if __name__ == "__main__":
    net = Net()
    #init(net, sys.argv[1])
    N, INPUTS, NUM_DATA, X, Y = allocate(net, sys.argv[1])
    calcWeights(net)
    
    for n in range(NUM_DATA):
        SimulateNet(net, INPUTS[n])

    crap,TEST,TEST_NUM,crap,crap = getFiles(sys.argv[2])
    
    for n in range(TEST_NUM):
        print "TESTING -----"
        SimulateNet(net, TEST[n])

