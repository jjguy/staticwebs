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

public class gameOfLife extends PApplet {

// Jeffrey J Guy
// 4 Oct 09
// jjg@jjguy.com
//
// Conway's Game of Life in Processing
// based on the game of life demo at http://ephexi.com/noc/gameoflife2/
// extended to allow user to create initial conditions and run the sim
//
// r to randomly initialize
// p to pause
// g to go
// s to single sTep
// c to clear 
// click/drag to modify the board
//


int cellsize = 15;
int COLS, ROWS;
//game of life board
int[][] old_board, new_board;
int bGo = 65535;
int bgColor = 30;
PFont font24;
String outString; 
int fade = 0;
int last = 0;
int iteration = 0;


public void setup()
{
  size(800, 500);
  smooth();
  //initialize rows, columns and set-up arrays
  COLS = width/cellsize;
  ROWS = height/cellsize;
  old_board = new int[COLS][ROWS];
  new_board = new int[COLS][ROWS];
  colorMode(RGB,255,255,255,100);
  background(bgColor);
  //call function to fill array with random values 0 or 1
  initBoard(false);
  frameRate(15);
  font24 = loadFont("AppleGothic-24.vlw");
  textMode(SCREEN);
  textAlign(LEFT, TOP);
}

public void draw()
{ 
  grid();
  textFont(font24);
  text("Iteration " + iteration, 10, 475); 
  
  if (keyPressed) checkKeys();
  
  fill(30,40);
  rectMode(CORNER);
  rect(0,0,width, height);  

  if (bGo > 0)
  {
    check(); 
    iteration++; 
    bGo--;
  }
 
  render();
  
  if (fade > 0) { 
    fill(255, fade);
    textFont(font24);    
    text(outString, 10, 10);
  }

  fade -= (millis() - last) / 7;
  last = millis(); 
}

public void grid() {
  for (int a=0; a<=COLS; a++) {
    for (int b=0; b<=ROWS; b++) {
      stroke(45);
      noFill();
      rectMode(CENTER);
      rect(a*cellsize, b*cellsize, cellsize, cellsize);    
    }
  }
 
}

public void check() {
  //loop through every spot in our 2D array and check spots neighbors
  for (int x = 0; x < COLS;x++) {
    for (int y = 0; y < ROWS;y++) {
      int nb = 0;
      //Note the use of mod ("%") below to ensure that cells on the edges have "wrap-around" neighbors
      
      //above row
      if (old_board[(x+COLS-1) % COLS ][(y+ROWS-1) % ROWS ] == 1) { nb++; }
      if (old_board[ x                ][(y+ROWS-1) % ROWS ] == 1) { nb++; }
      if (old_board[(x+1)      % COLS ][(y+ROWS-1) % ROWS ] == 1) { nb++; }
      
      //middle row
      if (old_board[(x+COLS-1) % COLS ][ y                ] == 1) { nb++; }
      if (old_board[(x+1)      % COLS ][ y                ] == 1) { nb++; }
      
      //bottom row
      if (old_board[(x+COLS-1) % COLS ][(y+1)      % ROWS ] == 1) { nb++; }
      if (old_board[ x                ][(y+1)      % ROWS ] == 1) { nb++; }
      if (old_board[(x+1)      % COLS ][(y+1)      % ROWS ] == 1) { nb++; }
       
    //RULES OF "LIFE" HERE
    if      ((old_board[x][y] == 1) && (nb <  2)) { new_board[x][y] = 0; }   
    else if ((old_board[x][y] == 1) && (nb >  3)) { new_board[x][y] = 0; }    
    else if ((old_board[x][y] == 0) && (nb == 3)) { new_board[x][y] = 1; }    
    else                                          { new_board[x][y] = old_board[x][y]; }
    }
  } 
  
  int[][] tmp = old_board;
  old_board = new_board;
  new_board = tmp;  
}

public void render() 
{
  //RENDER game of life based on "new_board" values
  for ( int i = 0; i < COLS;i++) 
  {
    for ( int j = 0; j < ROWS;j++) 
    {
      if ((new_board[i][j] == 1)) 
      {
        fill(255);
        noStroke();
        ellipse(i*cellsize,j*cellsize,cellsize,cellsize);
      }
    }
  }
  //swap old and new game of life boards
}

//init board with random "alive" squares
public void initBoard(boolean bClear) 
{
  background(bgColor);
  for (int i =0;i < COLS;i++) 
  {
    for (int j =0;j < ROWS;j++) 
    {
      if (random(5) <= 1.5f && !bClear) 
      {
        old_board[i][j] = 1;
        new_board[i][j] = 1;
      } 
      else 
      {
        old_board[i][j] = 0;
        new_board[i][j] = 0;
      }
    }
  }
}

public void updateText(String s)
{
  fade = 255;
  last = millis(); 
  outString = s;

  return;
}

public void checkKeys()
{
  if(key == 'p' || key == 'P')
  {
    updateText("Paused.");
    bGo = 0;
  }
  if (key == 'g' || key == 'G')
  {
    updateText("Go!");
    bGo = 65535;
  }
  if (key == 'r' || key == 'R')
  {
    updateText("Random init...");
    initBoard(false);
    iteration = 0;
    grid();
  }
  if (key == 'c' || key == 'C')
  {
    updateText("Cleared.");
    initBoard(true);
    iteration = 0;
    grid();
  }
  if (key == 's' || key == 'S')
  {
    updateText("Single step.");
    bGo = 1;
  }
}

public void mousePressed()
{
   if (mouseX<width && mouseX >0 && mouseY <height && mouseY > 0) 
   {
     int new_x;
     int new_y;
  
     new_x = mouseX/cellsize;
     new_y = mouseY/cellsize;
     if (new_board[new_x][new_y] == 0)
     {
       old_board[new_x][new_y] = 1;
       new_board[new_x][new_y] = 1;
     }
     else
     {
       old_board[new_x][new_y] = 0;
       new_board[new_x][new_y] = 0;
     }
     render();
  }  
}
public void mouseDragged() {
  if (mouseX<width && mouseX >0 && mouseY <height && mouseY > 0) 
  {
     int new_x;
     int new_y;
  
     new_x = mouseX/cellsize;
     new_y = mouseY/cellsize;
   
     old_board[new_x][new_y] = 1;
     new_board[new_x][new_y] = 1;
   
     render();
  }
}


  static public void main(String args[]) {
    PApplet.main(new String[] { "--bgcolor=#FFFFFF", "gameOfLife" });
  }
}
