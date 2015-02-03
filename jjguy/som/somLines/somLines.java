import processing.core.*; 
import processing.xml.*; 

import java.applet.*; 
import java.awt.*; 
import java.awt.image.*; 
import java.awt.event.*; 
import java.io.*; 
import java.net.*; 
import java.text.*; 
import java.util.*; 
import java.util.zip.*; 
import java.util.regex.*; 

public class somLines extends PApplet {

/* 
Kohonen's Self Organizing Map implementation

Based in spirit on Paras Chopra's python implementation
http://www.paraschopra.com/sourcecode/SOM/index.php

In this version, we use a 2-element vector and plot each node
based on their x/y value. (between 0 - 1)

g to go
p to pause
r to reset
q/w to lower/raise radius
a/s to lower/raise learning rate
z/x to lower/raise iteration count

5 Nov 08
Jeffrey J. Guy
jjg@case.edu
*/

SOM som;
int iter;
int maxIters = 1000;
boolean bDebug = true;
int screenW = 600;
int screenH = 600;
boolean bGo = false;
int fade = 0;
int last = 0;
String outString;
PFont font24;
PFont font12;
float radiusDecay;
float learnDecay;

public void setup() 
{
  size(screenW, screenH+50);
  background(255);
  frameRate(32);
  
  som = new SOM(15, 15, 2);
  som.initTraining(maxIters);

  iter = 1;
  
  font24 = loadFont("AppleGothic-24.vlw");
  font12 = loadFont("AppleGothic-12.vlw");
  textMode(SCREEN);
  textAlign(LEFT, TOP);
  learnDecay = som.learnRate;
  radiusDecay = (som.mapWidth + som.mapHeight) / 2;

}

public void draw()
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
    if (key == 'x' || key == 'X') {
      maxIters += 100;
      som.initTraining(maxIters);
    }
    if (key == 'z' || key == 'Z') {
      maxIters -= 100;
      som.initTraining(maxIters);
    }
    if (key == 's' || key == 'S') {
       som.learnRate += 0.01f;
       learnDecay = som.learnRate;
    }
    if (key == 'a' || key == 'A') {
       som.learnRate -= 0.01f;
       learnDecay = som.learnRate;
    }
    if (key == 'w' || key == 'Q') {
       som.radius += 1;
       radiusDecay = som.radius;
    }
    if (key == 'q' || key == 'W') {
       som.radius -= 1;
       radiusDecay = som.radius;
    }

    if (key == 'r' || key == 'R' ) {
      setup();
      bGo = false;
      updateText("Reset");
      learnDecay = som.learnRate;
      radiusDecay = (som.mapWidth + som.mapHeight) / 2;
    }  
  }
  
  float[] xy = new float[2];
  xy[0] = random(0, 1);
  xy[1] = random(0, 1);

  
  if (iter < maxIters && bGo){
    som.train(iter, xy);
    iter++;
  }
  
  background(255);
  som.render();  
  
  fill(0);
  rect(0, 600, 600, 50); 
  textFont(font12);
  fill(255);
  text("Radius:   "+radiusDecay, 480, 605);
  text("Learning: " +learnDecay, 480, 620);
  text("Iteration " + iter + "/" +maxIters, 480, 635);    

  if (fade > 0) { 
    fill(255, fade);
    textFont(font24);    
    text(outString, 10, 610);
  }

  fade -= (millis() - last) / 7;
  last = millis();
}

public void updateText(String s)
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
 float learnRate = 0.05f;
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
 
 public void initTraining(int iterations)
 {
   timeConstant = iterations/log(radius);   
 }
 
 public void train(int i, float w[])
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
 
 public float distance(Node node1, Node node2)
 {
   return sqrt( sq(node1.x - node2.x) + sq(node1.y - node2.y) );
 }
 
 public int bestMatch(float w[])
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
 
 public float weight_distance(float x[], float y[])
 {
    if (x.length != y.length) {
      println ("Error in SOM::distance(): array lens don't match");
      exit();
    }
    float tmp = 0.0f;
    for(int i = 0; i < x.length; i++)
       tmp += sq( (x[i] - y[i]));
    tmp = sqrt(tmp);
    return tmp;
 }
 
 public void render()
 {
   int gridX = screenW / mapWidth;
   int gridY = screenH / mapHeight;
   
   smooth();   
   for (int i = 0; i < mapWidth; i++) {
     for (int j = 0; j < mapHeight; j++) {
       ellipseMode(CENTER);
       fill(255, 0, 0);
       int nodeX = PApplet.parseInt(nodes[i][j].w[0]*600);
       int nodeY = PApplet.parseInt(nodes[i][j].w[1]*600);
       ellipse(nodeX, nodeY, 5, 5);
       if (j < mapHeight - 1)
       {
         stroke(0);
         line(nodeX, nodeY, nodes[i][j+1].w[0]*600, nodes[i][j+1].w[1]*600);
       }
       if (i < mapHeight - 1)
       {
         stroke(0);
         line(nodeX, nodeY, nodes[i+1][j].w[0]*600, nodes[i+1][j].w[1]*600);
       }
     } //for j
   } //for i
   
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
      w[i] = random(0.2f, 0.8f);
    }
  }
}

  static public void main(String args[]) {
    PApplet.main(new String[] { "somLines" });
  }
}
