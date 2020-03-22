<?php

$mapSize = 4096;

?><!DOCTYPE html>
<!--

Awakening - Wurm Unlimited Server
Map Viewer
Copyright (c) 2019 Per Löwgren

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

-->
<html>
<head>
<title>Awakening - Map</title>
<meta charset="UTF-8">
<link rel="stylesheet" type="text/css" href="main.css"/>
</head>
<body>
<main id="container">
  <canvas id="map" width="<?= $mapSize ?>" height="<?= $mapSize ?>"></canvas>
  <div id="markers"></div>
  <div id="sidebar">
    <h2><a href="/">Awakening</a></h2>
    <div id="zoom" class="panel">
      <h3>Zoom</h3>
      <div id="zoom-in">+</div>
      <div id="zoom-out">-</div>
      <p id="zoom-scale"></p>
    </div>
    <div id="map-type" class="panel">
      <h3>Map Type</h3>
      <div id="map-terrain" class="selected">Terrain</div>
      <div id="map-topographic">Topographic</div>
      <div id="map-isometric">Isometric</div>
    </div>
    <div id="coords" class="panel">
      <h3>Coordinates</h3>
      <p id="coords-pointer" style="display:none"></p>
      <p id="coords-mouse">0, 0</p>
      <p id="coords-distance" style="display:none"></p>
    </div>
    <div class="panel">
      <h3>Layers</h3>
      <label><input type="checkbox" id="layer-deeds" />Deeds</label>
      <label><input type="checkbox" id="layer-guardtowers" />Guard towers</label>
      <label><input type="checkbox" id="layer-highways" disabled />Highways</label>
      <label><input type="checkbox" id="layer-bridges" disabled />Bridges</label>
      <label><input type="checkbox" id="layer-tunnels" disabled />Tunnels</label>
    </div>
    <div class="panel">
      <h3>Info</h3>
      <p id="info"></p>
    </div>
    <footer>
      <div id="timestamp"></div>
      <a id="map-file" href="#" target="_blank">Map file</a>
    </footer>
  </div>
  <div id="search">
    <input type="text" id="searchbox" placeholder="Search deeds and locations..." />
    <div id="searchbutton"></div>
    <div id="autocomplete" style="display: none;"></div>
  </div>
</main>
<script type="text/javascript" src="config.js"></script>
<script type="text/javascript" src="map.js"></script>
</body>
</html>
