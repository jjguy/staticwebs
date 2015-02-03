/* 
Kohonen's Self Organizing Map implementation

Based in spirit on Paras Chopra's python implementation
http://www.paraschopra.com/sourcecode/SOM/index.php

In this version, we use a 3-element vector and visualize 
the weights of each node as a color.  To train, we feed
a random choice of one of ten colors.

g to go
p to pause
r to reset

5 Nov 08
Jeffrey J. Guy
jjg@case.edu
http://www.jjguy.com/som/
*/

SOM som;
int iter;
int maxIters = 5000;
int screenW = 600;
int screenH = 600;

boolean bDebug = true;
float learnDecay;
float radiusDecay;
PFont font12;
PFont font24;
boolean bGo = false;
String outString;
int fade = 0;
int last = 0;


void setup() 
{
  size(screenW, screenH+50, P3D);
  background(0);
  frameRate(32);
  
  som = new SOM(40, 40, 3);
  som.initTraining(maxIters);
  iter = 1;
  
  font12 = loadFont("AppleGothic-12.vlw");
  font24 = loadFont("AppleGothic-24.vlw");
  textMode(SCREEN);
  textAlign(LEFT, TOP);
  learnDecay = som.learnRate;
  radiusDecay = (som.mapWidth + som.mapHeight) / 2;
}

void draw()
{

  if(keyPressed) {
    if (key == 'g' || key == 'G') {
      bGo = true;
      updateText("Go!");      
    }
    if (key == 'p' || key == 'P') {
      bGo = false;
      updateText("Paused...");      
    }
    if (key == 'r' || key == 'R' ) {
      setup();
      bGo = false;
      updateText("Reset");
      learnDecay = som.learnRate;
      radiusDecay = (som.mapWidth + som.mapHeight) / 2;
    }  
  }
  
  float[] rgb = new float[3];
  //rgb[0] = ((random(255) / 50) * 50) / 255.0;
  //rgb[1] = ((random(255) / 50) * 50) / 255.0;
  //rgb[2] = ((random(255) / 50) * 50) / 255.0;

  rgb[0] = random(255) / 255.0;
  rgb[1] = random(255) / 255.0;
  rgb[2] = random(255) / 255.0;

  float[][] rgb2 = new float[10][3] ;
  rgb2[0][0] = 1.0; rgb2[0][1] = 1.0; rgb2[0][2] = 1.0;
  rgb2[1][0] = 0.0; rgb2[1][1] = 0.0; rgb2[1][2] = 0.0;
  rgb2[2][0] = 1.0; rgb2[2][1] = 0.0; rgb2[2][2] = 1.0;
  rgb2[3][0] = 1.0; rgb2[3][1] = 0.0; rgb2[3][2] = 0.0;
  rgb2[4][0] = 0.0; rgb2[4][1] = 1.0; rgb2[4][2] = 0.0;
  rgb2[5][0] = 0.0; rgb2[5][1] = 0.0; rgb2[5][2] = 1.0;
  rgb2[6][0] = 1.0; rgb2[6][1] = 1.0; rgb2[6][2] = 0.0;
  rgb2[7][0] = 0.0; rgb2[7][1] = 1.0; rgb2[7][2] = 1.0;
  rgb2[8][0] = 1.0; rgb2[8][1] = 0.4; rgb2[8][2] = 0.4;
  rgb2[9][0] = 0.25; rgb2[9][1] = 0.25; rgb2[9][2] = 0.25;
      
  int t = int(random(10));
  if (iter < maxIters && bGo){
    som.train(iter, rgb2[t]);
    //som.train(iter, rgb);
    iter++;
  }
  
  background(0);
  som.render();  
  fill(0);
  rect(0, screenH, screenW, 35);
  for (int i = 0; i<10; i++)
  {
    stroke(255);
    int r = int(rgb2[i][0]*255);
    int g = int(rgb2[i][1]*255);
    int b = int(rgb2[i][2]*255);
    fill(r, g, b);
    rect(i*35, screenH+5, 25, 25);
  }  
   
  fill(255);
  textFont(font12);
  text("Radius:   "+radiusDecay, 450, 605);
  text("Learning: " +learnDecay, 450, 620);
  text("Iteration " + iter + "/" +maxIters, 450, 635);

  if (fade > 0) { 
    fill(255, fade);
    textFont(font24);    
    text(outString, 350, 610);
  }

  fade -= (millis() - last) / 7;
  last = millis();  
}

void updateText(String s)
{
  fade = 255;
  last = millis(); 
  outString = s;

  return;
}

class SOM
{
 int mapWidth;
 int mapHeight;
 Node[][] nodes;
 float radius;
 float timeConstant;
 float learnRate = 0.05;
 int inputDimension;
 
 SOM(int h, int w, int n)
 {
   mapWidth = w;
   mapHeight = h;
   radius = (h + w) / 2;
   inputDimension = n;
   
   nodes = new Node[h][w];
   // create nodes/initilize map
   for(int i = 0; i < h; i++){
     for(int j = 0; j < w; j++) {
       nodes[i][j] = new Node(n, h, w);
       nodes[i][j].x = i;
       nodes[i][j].y = j;
     }//for j
   }//for i
    
 } 
 
 void initTraining(int iterations)
 {
   timeConstant = iterations/log(radius);   
 }
 
 void train(int i, float w[])
 {   
   radiusDecay = radius*exp(-1*i/timeConstant);
   learnDecay = learnRate*exp(-1*i/timeConstant);
   
   //get best matching unit
   int ndxComposite = bestMatch(w);
   int x = ndxComposite >> 16;
   int y = ndxComposite & 0x0000FFFF;

   //if (bDebug) println("bestMatch: " + x + ", " + y + " ndx: " + ndxComposite);
   
 
   //scale best match and neighbors...
   for(int a = 0; a < mapHeight; a++) {
     for(int b = 0; b < mapWidth; b++) {
       
        //float d = distance(nodes[x][y], nodes[a][b]);
        float d = dist(nodes[x][y].x, nodes[x][y].y, nodes[a][b].x, nodes[a][b].y);
        float influence = exp((-1*sq(d)) / (2*radiusDecay*i));
        //println("Best Node: ("+x+", "+y+") Current Node ("+a+", "+b+") distance: "+d+" radiusDecay: "+radiusDecay);
        
        if (d < radiusDecay)          
          for(int k = 0; k < inputDimension; k++)
            nodes[a][b].w[k] += influence*learnDecay*(w[k] - nodes[a][b].w[k]);
        
     } //for j
   } // for i
  
 } // train()
 
 float distance(Node node1, Node node2)
 {
   return sqrt( sq(node1.x - node2.x) + sq(node1.y - node2.y) );
 }
 
 int bestMatch(float w[])
 {
   float minDist = sqrt(inputDimension);
   int minIndex = 0;
   
   for (int i = 0; i < mapHeight; i++) {
     for (int j = 0; j < mapWidth; j++) {
       float tmp = weight_distance(nodes[i][j].w, w);
       if (tmp < minDist) {
         minDist = tmp;
         minIndex = (i << 16) + j;
       }  //if
     } //for j
   } //for i
   
  // note this index is x << 16 + y. 
  return minIndex;
 }
 
 float weight_distance(float x[], float y[])
 {
    if (x.length != y.length) {
      println ("Error in SOM::distance(): array lens don't match");
      exit();
    }
    float tmp = 0.0;
    for(int i = 0; i < x.length; i++)
       tmp += sq( (x[i] - y[i]));
    tmp = sqrt(tmp);
    return tmp;
 }
 
 void render()
 {
   int pixPerNodeW = screenW / mapWidth;
   int pixPerNodeH = screenH / mapHeight;
   
   for(int i = 0; i < mapWidth; i++) {
     for(int j = 0; j < mapHeight; j++) {
       int r = int(nodes[i][j].w[0]*255);
       int g = int(nodes[i][j].w[1]*255);
       int b = int(nodes[i][j].w[2]*255);
       fill(r, g, b);
       stroke(0);
       rectMode(CORNER);
       rect(i*pixPerNodeW, j*pixPerNodeH, pixPerNodeW, pixPerNodeH); 
       //if (bDebug) println("Writing rect ("+r+", "+g+", "+b+") at ("+i*pixPerNodeW+", "+ j*pixPerNodeH+")");
     } // for j
   } // for i
 } // render()
 
}

class Node
{
  int x, y; 
  int weightCount;
  float [] w; 
  Node(int n, int X, int Y)
  {
    x = X;
    y = Y;
    weightCount = n;
    w = new float[weightCount];
    
    // initialize weights to random values
    for(int i = 0; i < weightCount; i++) 
    {
      w[i] = random(0.25, 0.75);
    }
  }
}
