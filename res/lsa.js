// Locate Soul Assitant (LSA for short)
// Originaly Made by Bannaner, Updates and GUI by Axeblade346 
//____________________________________________________________

// Message to SERVER OWNERS from Axeblade346
// Programs / Scripts like this has been around since I originaly made the searchable Delevirance map website back in late 2011.
// I used tansparent image overlays to mark the deeds, others basicly used my code and just overlay images of archs.
// I have seen it pop up on a russian site for another games web map (and their code works on our webmaps withy a few oddities) and a few days later Bannaner showed me his version.
// As this can`t be deteced and can`t be stopped I deciced to level the playing field and make an easy to use public version so that all the players can be on a level playing field.
//______________


//This uses Canvas to draws an filled in arc segment over the map to assist you with locate soul and any thing that uses locate soul as a base like Treasure maps (Public Mod).
//Overlaying a few of these arcs assist in narrowing down the area alot faster untill you get close. 4 locate soul cast from the corners of a map and this could narrow down a corpse to within 40 tiles on a 4096 x 4096 map.
// Tested with the OLD Wyvern WEB Map and the recent Kenabil Web Map (https://github.com/awakening-wurm/MapViewer)
//______________

// Thanks to The Scallywag server (Owner:Killem) for allowing me test on their server and the players for thier feedback.
//______________


//Version Notes.
//1 - Base code done by Bannaner
//2 - Updated it from a GreaseMonkey to Javascript for use in chrome - Axeblade346
//3 - Added usage comments and guide - Axeblade346
//4 - Added the ls funtion so only X Y and event message is needed - Axeblade346
//Todo:
// Intergration into the Kenabil Web Map
// User interface so console is not needed
// Figure out how to clear the canvis without clearing the maps images too.
//______________



//For ease of use always look North and use 0 as the facingdirection
//xpos = Your X position on the map (can used decimals)
//ypos = Your Y position on the map (can used decimals)
//facingdirection = The Angle you are looking in (compass angle like 36.9, not N S E W)
//scrolldirection works on a number system
//-- In Front = 0
//-- Ahead right = 1
//-- Right = 2
//-- Behind Right = 3
//-- Behind = 4
//-- Behind Left = 5
//-- Left = 6
//-- Ahead Left = 7
//scrolldistancemin = Minimum range of Locate soul as per https://www.wurmpedia.com/index.php/Locate_soul#Distances
//scrolldistancemax = Maximum range of Locate soul as per https://www.wurmpedia.com/index.php/Locate_soul#Distances

function drawLocateSoul(xpos, ypos, facingdirection, scrolldirection, scrolldistancemin, scrolldistancemax){
		
	var canvas = document.getElementById('map')
	var can = canvas.getContext('2d');

	//Draw the closer distance
	var startang = (-facingdirection + scrolldirection*45 - 90 - 22.5) * Math.PI/180;
	var endang = (-facingdirection + scrolldirection*45 - 90 + 22.5) * Math.PI/180;
	can.beginPath();
		can.arc(xpos, ypos, scrolldistancemin, startang, endang, false)
		//Draw the closer distance
		can.arc(xpos, ypos, scrolldistancemax, endang, startang, true)
		//Go back to the starting arc but with no arcing, just a point to close the shape
		can.arc(xpos, ypos, scrolldistancemin, startang, startang, false)
	can.closePath()

	can.strokeStyle = 'red';
	can.lineWidth = 1;
	can.globalAlpha = 1;
	can.globalCompositeOperation='source-over';
	can.stroke();
	can.globalAlpha = 0.1;
	can.fillStyle = 'yellow';
	can.globalCompositeOperation='source-atop';
	can.fill();
	can.fillStyle = 'green';
	can.globalCompositeOperation='destination-over';
	can.fill();
	can.globalAlpha = 1;
  
	//Draw a blue centre line to assits with direct directions like lightning marking a spot.
	var midang = (-facingdirection + scrolldirection*45 - 90) * Math.PI/180;
	can.beginPath();
		can.arc(xpos, ypos, scrolldistancemin, midang, midang, false)
		can.arc(xpos, ypos, scrolldistancemax, midang, midang, false)
	can.closePath()

	can.strokeStyle = 'blue';
	can.lineWidth = 1;
	can.globalAlpha = 0.3;
	can.globalCompositeOperation='source-over';
	can.stroke();
}

//______________

//This allows the user to just enter the X and Y and eventmessage and accepts the player is looking NORTH

//Note the order of the if/elseif is spesificly odd due to the fact that that some strings contain the same words

//directions
//0 = The marked spot is in front of you a stone's throw away!
//1 = The marked spot is ahead of you to the right a stone's throw away! 
//2 = The marked spot is to the right of you a stone's throw away! 
//3 = The marked spot is behind you to the right very close. 
//4 = The marked spot is quite some distance away behind you. 
//5 = The marked spot is quite some distance away behind you to the left.
//6 = The marked spot is pretty far away to the left of you. 
//7 = The marked spot is ahead of you to the left fairly close by.
//----
//distances
// 0 tiles				You are practically standing on the <player>!
// 1-3 tiles			The <player> is in front of you a stone's throw away!
// 4-5 tiles			The <player> is in front of you very close.
// 6-9 tiles			The <player> is in front of you pretty close by.
// 10-19 tiles			The <player> is in front of you fairly close by.
// 20-49 tiles			The <player> is some distance away in front of you.
// 50-199 tiles			The <player> is quite some distance away in front of you.
// 200-499 tiles		The <player> is rather a long distance away in front of you.
// 500-999 tiles		The <player> is pretty far away in front of you.
// 1000 to 1999 tiles	The <player> is far away in front of you.
// 2000+				The <player> is very far away in front of you.

function ls (x,y,eventmessage) {
	
	let str = eventmessage;
	var sd = 0;
	var smin = 0;
	var smax = 0;
		if (str.includes("ahead of you to the right")==true){
			sd=1;
		}else if (str.includes("behind you to the right")==true){
			sd=3;
		}else if (str.includes("behind you to the left")==true){
			sd=5;
		}else if (str.includes("ahead of you to the left")==true){
			sd=7;
		}else if (str.includes("in front of you")==true){
			sd=0;
		}else if (str.includes("right of you")==true){
			sd=2;
		}else if (str.includes("behind you")==true){
		sd=4;
		}else if (str.includes("left of you")==true){
		sd=6;
		}

		if (str.includes("practically standing")==true){
			smin=0;
			smax=0;
		}else if (str.includes("stone's throw away")==true){
			smin=1;
			smax=3;
		}else if (str.includes("very close")==true){
			smin=4;
			smax=5;
		}else if (str.includes("pretty close by")==true){
			smin=6;
			smax=9;
		}else if (str.includes("fairly close by")==true){
			smin=10;
			smax=19;
		}else if (str.includes("quite some distance")==true){
			smin=50;
			smax=199;
		}else if (str.includes("rather a long distance")==true){
			smin=200;
			smax=499;
		}else if (str.includes("pretty far away")==true){
			smin=500;
			smax=999;
		}else if (str.includes("very far away")==true){
			smin=2000;
			smax=10000;
		}else if (str.includes("some distance")==true){
			smin=20;
			smax=49;
		}else if (str.includes("far away")==true){
			smin=1000;
			smax=1999;
		}
drawLocateSoul(x,y,0,sd,smin,smax)		
}



