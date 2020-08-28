

(function(global) {

	const serverInfo = '<h4>Awakening</h4>'+
	                   'Wurm Unlimited Server<br>';

	const zoomLevels = [ 0.25, 0.375, 0.5, 0.75, 1.0, 1.25, 1.5, 2, 3, 4, 6, 8 ];
	const zoomScales = [ '25%', '37.5%', '50%', '75%', '100%', '125%', '150%', '200%', '300%', '400%', '600%', '800%' ];

	function getWidth() {
		return global.innerWidth || document.documentElement.clientWidth || document.body.clientWidth;
	}

	function getHeight() {
		return global.innerHeight || document.documentElement.clientHeight || document.body.clientHeight;
	}

	function getDate(timestamp) {
		let date = new Date(timestamp);
		let yy = date.getFullYear();
		let mm = date.getMonth()+1;
		let dd = date.getDate();
		return yy+'-'+(mm<=9? '0' : '')+mm+'-'+(dd<=9? '0' : '')+dd;
	}

	function parseQueryString(str) {
		let params = {};
		if(str) {
			let a = str.split('&');
			for(let i=0; i<a.length; ++i) {
				let b = a[i].split('=');
				params[b[0]] = b[1];
			}
		}
		return params;
	}

	function MapImage(url) {
		this.url       = url;
		this.loaded    = false;
		this.attempts  = 0;
		this.img       = null;
	}

	function MapLoader(map,mapImage) {
		this.map = map;
		this.mapImage = mapImage;
		map.mapImage = mapImage;
		this.load = function() {
			let img = new Image();
			img.mapImage = this.mapImage;
			img.loader = this;
			img.onload = function() {
				this.mapImage.img = this;
				this.mapImage.loaded = true;
				this.loader.map.draw();
			}
			let fail = function() {
				this.mapImage.attempts++;
				if(this.mapImage.attempts<4) {
					this.loader.load(this.mapImage);
				}
			}
			img.onerror = fail;
			img.onabort = fail;
			img.src = mapImage.url;
		}
	}

	function Marker(type,x,y,height,element) {
		this.type     = type;
		this.x        = x;
		this.y        = y;
		this.height   = height;
		this.element  = element;
	}

	function Map(config,deeds,focusZones,guardTowers) {
		this.config      = config;
		this.mapImage    = null;
		this.deeds       = deeds;
		this.focusZones  = focusZones;
		this.guardTowers = guardTowers;
		this.mode        = config.mode;
		this.focusZoneTypes = [
			'none',
			'volcano',
			'PvP zone',
			'name',
			'name',
			'PvE zone',
			'PvP: HotA',
			'PvP: battlecamp',
			'flatten dirt',
			'house, wood',
			'house, stone',
			'premium spawn',
			'no-build',
			'tall walls',
			'fog',
			'flatten rock'
		];
		this.x          = config.x;
		this.y          = config.y;
		this.zoomIndex  = 4;
		this.zoom       = zoomLevels[this.zoomIndex];
		this.mx         = 0;
		this.my         = 0;
		this.md         = false;
		this.mm         = false;
		this.showDeeds        = true;
		this.showGuardTowers  = false;
		this.showHighways     = false;
		this.showBridges      = false;
		this.showTunnels      = false;
		this.markers = [];
		this.pointer = new Marker('pointer',-1,-1,0,null);

		this.config.canvas.setAttribute('width',this.config.size);
		this.config.canvas.setAttribute('height',this.config.size);
		this.config.ctx.imageSmoothingEnabled = false;
		this.config.info.innerHTML = serverInfo;

		this.updateHash = function(x,y,d) {
			if(x!=-1 && y!=-1) global.location.hash = 'x='+this.pointer.x+'&y='+this.pointer.y;
			else if(d) global.location.hash = 'deed='+d;
			else global.location.hash = '';
		}

		this.updateMarker = function(marker) {
			let x = ((marker.x+0.5)*this.zoom)-128;
			let y = ((marker.y+0.5)*this.zoom)-128;
			if(this.mode=='isometric') y -= marker.height*(this.zoom/40);
			x = Math.round(x);
			y = Math.round(y);
			if(this.showDeeds===false && marker.type=='deed') marker.element.style.display = 'none';
			else marker.element.setAttribute('style','display: block; top: '+y+'px; left: '+x+'px;');
			if(marker.border!=undefined) {
				let w = Math.round(((1+marker.deed.ex-marker.deed.sx)*this.zoom)-2);
				let h = Math.round(((1+marker.deed.ey-marker.deed.sy)*this.zoom)-2);
				let l = Math.floor(128+((marker.deed.sx-marker.deed.x-0.5)*this.zoom));
				let t = Math.floor(128+((marker.deed.sy-marker.deed.y-0.5)*this.zoom));
				if(marker.label) marker.label.setAttribute('style','top: '+(y+t)+'px; left: '+(x+l)+'px;');
				if(marker.border) marker.border.setAttribute('style','top: '+t+'px;  left: '+l+'px; width: '+w+'px; height: '+h+'px;');
				if(marker.bounds) marker.bounds.setAttribute('style','top: '+(y+t)+'px;  left: '+(x+l)+'px; width: '+w+'px; height: '+h+'px;');
			}
		}

		this.updatePointer = function() {
			if(this.pointer.x!=-1 && this.pointer.y!=-1) {
				let x = Math.round(((this.pointer.x+0.5)*this.zoom)-13);
				let y = Math.round(((this.pointer.y+0.5)*this.zoom)-23);
				this.pointer.element.setAttribute('style','display: block; top: '+y+'px; left: '+x+'px;');
			}
		}

		this.updateMarkers = function() {
			for(let i=0; i<this.markers.length; ++i)
				this.updateMarker(this.markers[i]);
			for(let i=0; i<this.guardTowers.length; ++i) {
				let guardTower = this.guardTowers[i];
				if(this.showGuardTowers===false) guardTower.element.style.display = 'none';
				else {
					let x = Math.round(((guardTower.x+0.5)*this.zoom)-11);
					let y = Math.round(((guardTower.y+0.5)*this.zoom)-22);
					if(this.mode=='isometric') y -= guardTower.z*(this.zoom/40);
					guardTower.element.setAttribute('style','display: block; top: '+y+'px; left: '+x+'px;');
				}
			}
			this.updatePointer();
		}

		this.createFocusZoneBorder = function(marker,focusZone) {
			if((this.mode=='terrain' && !this.config.showDeedBordersInFlatMode) ||
				(this.mode=='topographic' && !this.config.showDeedBordersInFlatMode) ||
				(this.mode=='isometric' && !this.config.showDeedBordersIn3dMode)) return false;
			marker.border = document.createElement('div');
			marker.border.setAttribute('class','border fzb_'+focusZone.type);
//			marker.border.setAttribute('title', focusZone.name);
			let map = this;
			let infoText = '<h4>'+focusZone.name+'</h4>Type: '+this.focusZoneTypes[focusZone.type];
			marker.border.addEventListener('mouseover',function(e) {
				let info = map.config.info;
				info.innerHTML = infoText;
			});
			marker.border.addEventListener('mouseout',function(e) {
				let info = map.config.info;
				info.innerHTML = serverInfo;
			});
			marker.deed = focusZone;
			return true;
		}

		this.createDeedBounds = function(marker,deed) {
			marker.bounds = document.createElement('div');
			marker.bounds.setAttribute('class','bounds');
			let map = this;
			let type = deed.permanent? 'permanent deed' : 'deed';
			let kingdom = kingdoms[deed.kingdom];
			let infoText = '<h4>'+deed.name+'</h4>'+
			               'Type: '+type+'<br>'+
			               'Mayor: <i>'+deed.mayor+'</i><br>'+
			               'Kingdom: <i>'+kingdom.name+'</i><br>'+
			               (deed.mayor!=deed.founder? 'Founder: <i>'+deed.founder+'</i><br>' : '')+
			               'Creation date: '+getDate(deed.creationDate)+'<br>'+
			               'Token: '+deed.x+', '+deed.y+'<br>'+
			               'Size: '+(deed.ex-deed.sx+1)+', '+(deed.ey-deed.sy+1)+'<br>'+
			               '';
			marker.bounds.addEventListener('mouseover',function(e) {
				let info = map.config.info;
				info.innerHTML = infoText;
			});
			marker.bounds.addEventListener('mouseout',function(e) {
				let info = map.config.info;
				info.innerHTML = serverInfo;
			});
			return true;
		}

		this.draw = function() {
			if(this.mapImage===null) return;
			let ctx = this.config.ctx;
			ctx.drawImage(this.mapImage.img,0,0);
			this.config.mapFile.href = this.mapImage.url;
			if(this.showHighways) this.drawHighwayNodes(highwayNodes,"rgba(255,255,0,0.4)","#cc0000","#cc6600");
			if(this.showBridges) this.drawHighwayNodes(bridgeNodes,"rgba(255,153,255,0.4)","#cc0000","#cc6600");
			if(this.showTunnels) this.drawHighwayNodes(tunnelNodes,"rgba(0,255,255,0.4)","#cc0000","#cc6600");
		}

		this.drawHighwayNodes = function(nodes,highwayColor,waystoneBorder,waystoneColor) {
			let ctx = this.config.ctx;
			let z = this.mode=='isometric'? 1/40 : 0;
			ctx.strokeStyle = highwayColor;
			ctx.lineWidth = 2.5;
			ctx.lineCap = "round";
			ctx.beginPath();
			for(let i=0; i<nodes.length; ++i) {
				let n = nodes[i];
				if(n.length>=6) {
					let x1 = n[0],y1 = n[1]-z*n[2];
					let x2 = n[3],y2 = n[4]-z*n[5];
					ctx.moveTo(x1,y1);
					ctx.lineTo(x2,y2);
				}
			}
			ctx.stroke();
			ctx.lineWidth = 0.5;
			ctx.strokeStyle = waystoneBorder;
			ctx.fillStyle = waystoneColor;
			for(let i=0; i<nodes.length; ++i) {
				let n = nodes[i];
				if(n.length==4 || n.length==7) {
					let x1 = n[0],y1 = n[1]-z*n[2];
					ctx.beginPath();
					ctx.arc(x1,y1,3,0,2*Math.PI,false);
					ctx.fill();
					ctx.stroke();
				}
			}
		}

		this.go = function(x,y) {
			this.x = x*this.zoom;
			this.y = y*this.zoom;
			this.update();
		}

		this.mouseDown = function(mx,my) {
			this.config.list.style.display = 'none';
			this.mx = mx;
			this.my = my;
			this.md = true;
			this.mm = false;
		}

		this.mouseMove = function(mx,my) {
			if(!this.md) {
				mx = Math.floor((this.x+mx-(getWidth()/2))/this.zoom);
				my = Math.floor((this.y+my-(getHeight()/2))/this.zoom);
				this.config.coordsMouse.innerHTML = mx+', '+my;
				if(this.pointer.x!=-1 && this.pointer.y!=-1) {
					let dx = Math.abs(this.pointer.x-mx);
					let dy = Math.abs(this.pointer.y-my);
					let d = dx==0? dy : (dy==0? dx : Math.round(Math.sqrt(dx*dx+dy*dy)));
					this.config.coordsDistance.innerHTML = d+' ['+dx+', '+dy+']';
				}
				return;
			}
			let dx = mx-this.mx;
			let dy = my-this.my;
			this.mx = mx;
			this.my = my;
			this.mm = true;
			this.go((this.x-dx)/this.zoom,(this.y-dy)/this.zoom);
		}

		this.mouseUp = function() {
			if(this.md===false) return;
			this.md = false;
			if(this.mm===false) {
				let px = Math.floor((this.x+this.mx-(getWidth()/2))/this.zoom);
				let py = Math.floor((this.y+this.my-(getHeight()/2))/this.zoom);
				if(px==this.pointer.x && py==this.pointer.y) {
					px = -1;
					py = -1;
				}
				this.setPointer(px,py);
				this.updateHash(this.pointer.x,this.pointer.y);
			}
		}

		this.setPointer = function(px,py) {
			this.pointer.x = px;
			this.pointer.y = py;
			if(px!=-1 && py!=-1) {
				this.config.coordsPointer.style.display = 'block';
				this.config.coordsPointer.innerHTML = this.pointer.x+', '+this.pointer.y;
				this.config.coordsDistance.style.display = 'block';
				this.config.coordsDistance.innerHTML = '0 [0, 0]';
				this.updatePointer();
			} else {
				this.config.coordsPointer.style.display = 'none';
				this.config.coordsDistance.style.display = 'none';
				this.pointer.element.style.display = 'none';
			}
		}

		this.update = function() {
			this.clamp();
			let width = getWidth();
			let height = getHeight();
			let s = this.config.size*this.zoom;
			let style = 'width: '+s+'px; height: '+s+'px; top: '+Math.round((height/2)-this.y)+'px; left: '+Math.round((width/2)-this.x)+'px;';
			this.config.markers.setAttribute('style',style);
			if(Math.round(this.zoom)!=this.zoom) style += ' image-rendering: auto; -ms-interpolation-mode: auto;';
			this.config.canvas.setAttribute('style',style);
			this.config.markers.setAttribute('class','zoom'+this.zoomIndex);
			this.config.zoomScale.innerHTML = zoomScales[this.zoomIndex];
		}

		this.clamp = function() {
			if(this.x<0) this.x = 0;
			if(this.y<0) this.y = 0;
			if(this.x>=this.config.size*this.zoom) this.x = this.config.size*this.zoom;
			if(this.y>=this.config.size*this.zoom) this.y = this.config.size*this.zoom;
		}

		this.zoomIn = function(mx,my) {
			if(this.zoomIndex==zoomLevels.length-1) return;
			this.zoomIndex++;
			this.zoomUpdate(mx,my);
		}

		this.zoomOut = function(mx,my) {
			if(this.zoomIndex==0) return;
			this.zoomIndex--;
			this.zoomUpdate(mx,my);
		}

		this.zoomUpdate = function(mx,my) {
			let z = this.zoom,w = getWidth(),h = getHeight(),w2 = w*0.5,h2 = h*0.5;
			let cx = this.x/z,cy = this.y/z;
			this.zoom = zoomLevels[this.zoomIndex];
			if(mx===undefined && my===undefined) {
				mx = w2;
				my = h2;
			}
			this.x = ((cx-w2/z)+mx/z)*this.zoom-mx+w2;
			this.y = ((cy-h2/z)+my/z)*this.zoom-my+h2;
			this.updateMarkers();
			this.update();
		}

		this.autocomplete = function() {
			let text = this.config.searchbox.value.replace(/[^a-zA-Z]/g,'').toLowerCase();
			if(text=='') {
				this.config.list.setAttribute('style','display: none;');
				return;
			}
			this.config.list.setAttribute('style','display: block;');
			let html = '';
			for(let i=0; i<this.deeds.length; ++i)
				if(this.deeds[i].search.indexOf(text)===0)
					html += '<div onclick="javascript:config.map.gotoDeed('+i+');">'+this.deeds[i].name+'</div>';
			for(let i=0; i<this.deeds.length; ++i)
				if(this.deeds[i].search.indexOf(text)>=1)
					html += '<div onclick="javascript:config.map.gotoDeed('+i+');">'+this.deeds[i].name+'</div>';
			this.config.list.innerHTML = html;
		}

		this.searchDeed = function() {
			let text = this.config.searchbox.value.replace(/[^a-zA-Z]/g,'').toLowerCase();
			this.config.list.setAttribute('style','display: none;');
			for(let i=0; i<this.deeds.length; ++i)
				if(this.deeds[i].search.indexOf(text)===0) {
					this.gotoDeed(i);
					return;
				}
			for(let i=0; i<this.deeds.length; ++i)
				if(this.deeds[i].search.indexOf(text)>=1) {
					this.gotoDeed(i);
					return;
				}
		}

		this.getDeedIndex = function(d) {
			if(typeof d === 'string') {
				for(let i=0; i<this.deeds.length; ++i)
					if(this.deeds[i].search==d) return i;
				return 0;
			}
			return d;
		}

		this.gotoDeed = function(d) {
			d = this.getDeedIndex(d);
			this.config.list.setAttribute('style','display: none;');
			this.config.searchbox.value = '';
			let deed = this.deeds[d];
			this.setPointer(-1,-1);
			this.go(deed.x,deed.y);
			this.updateHash(-1,-1,deed.search);
		}

		this.updateLayer = function(key) {
			let layer = this.config.layers[key];
			let map = this;
			layer.checked = map[key];
			layer.addEventListener('change',function(e) {
				let checked = map.config.layers[key].checked;
				map[key] = checked;
				if(key=='showDeeds' || key=='showGuardTowers') map.updateMarkers();
				else if(key=='showHighways' || key=='showBridges' || key=='showTunnels') map.draw();
			});
		}

		this.load = function() {
			this.config.markers.innerHTML = '';
			this.markers = [];
			var mapImage = new MapImage('./map-'+this.mode+'.png');
			new MapLoader(this,mapImage).load();
			for(let i=0; i<this.deeds.length; ++i) {
				let deed = this.deeds[i];
				let name = deed.name;
				if(name.length>24) name = name.substring(0,22)+'...';
				let element = document.createElement('div');
				let marker = new Marker('deed',deed.x,deed.y,deed.height,element);
				deed.marker = marker;
				this.updateMarker(marker);
				if((this.mode=='terrain' && this.config.showDeedBordersInFlatMode) ||
					(this.mode=='topographic' && this.config.showDeedBordersInFlatMode) ||
					(this.mode=='isometric' && this.config.showDeedBordersIn3dMode)) {
					marker.border = document.createElement('div');
					marker.border.setAttribute('class',deed.permanent? 'border deed-permanent' : 'border deed-normal');
					marker.deed = deed;
					element.appendChild(marker.border);
				}
				let label = document.createElement('span');
				label.innerHTML = name;
				label.setAttribute('class',deed.permanent? 'deed deed-permanent' : 'deed deed-normal');
				element.setAttribute('class','marker');
				element.appendChild(label);
				this.config.markers.appendChild(element);
				this.markers.push(marker);
			}
			for(let i=0; i<this.guardTowers.length; ++i) {
				let guardTower = this.guardTowers[i];
				let element = document.createElement('div');
				element.setAttribute('class','tower');
				guardTower.element = element;
				this.config.markers.appendChild(element);
			}
			for(let i=0; i<this.focusZones.length; ++i) {
				let focusZone = this.focusZones[i];
				let name = focusZone.name;
				let element = document.createElement('div');
				let marker = new Marker('focusZone',focusZone.x,focusZone.y,focusZone.height,element);
				this.updateMarker(marker);
				if(this.createFocusZoneBorder(marker,focusZone))
					element.appendChild(marker.border);
				element.setAttribute('class','marker fzm_'+focusZone.type);
				this.config.markers.appendChild(element);
				this.markers.push(marker);
			}
			for(let i=0; i<this.deeds.length; ++i) {
				let deed = this.deeds[i];
				let marker = deed.marker;
				if(this.createDeedBounds(marker,deed))
					this.config.markers.appendChild(marker.bounds);
				deed.search = deed.name.replace(/[^a-zA-Z]/g,'').toLowerCase();
			}
			this.updateMarkers();
			for(let key in this.config.layers)
				this.updateLayer(key);

			let params = {};
			let element = document.createElement('div');
			element.className = 'pointer';
			element.style.display = 'none';
			this.config.markers.appendChild(element);
			this.pointer.element = element;
		}

		this.load();
		this.update();

		if(global.location.hash) {
			let params = parseQueryString(global.location.hash.substr(1));
			if(params.deed) {
				this.gotoDeed(params.deed);
			} else if(params.x && params.y) {
				let x = params.x*1.0;
				let y = params.y*1.0
				this.setPointer(x,y);
				this.go(x,y);
			}
		}
	}

	config.container       = document.getElementById('container');
	config.canvas          = document.getElementById('map');
	config.ctx             = config.canvas.getContext('2d');
	config.coordsMouse     = document.getElementById('coords-mouse');
	config.coordsPointer   = document.getElementById('coords-pointer');
	config.coordsDistance  = document.getElementById('coords-distance');
	config.markers         = document.getElementById('markers');
	config.zoomIn          = document.getElementById('zoom-in');
	config.zoomOut         = document.getElementById('zoom-out');
	config.zoomScale       = document.getElementById('zoom-scale');
	config.toggleTerrain   = document.getElementById('map-terrain');
	config.toggleTopo      = document.getElementById('map-topographic');
	config.toggleIso       = document.getElementById('map-isometric');
	config.layers          = {
		showDeeds:        document.getElementById('layer-deeds'),
		showGuardTowers:  document.getElementById('layer-guardtowers'),
		showHighways:     document.getElementById('layer-highways'),
		showBridges:      document.getElementById('layer-bridges'),
		showTunnels:      document.getElementById('layer-tunnels')
	};
	config.info            = document.getElementById('info');
	config.searchbox       = document.getElementById('searchbox');
	config.searchbutton    = document.getElementById('searchbutton');
	config.list            = document.getElementById('autocomplete');
	config.mapFile         = document.getElementById('map-file');
	config.timestamp       = document.getElementById('timestamp');

	var map = new Map(config,deeds,focusZones,guardTowers);

	container.addEventListener('wheel',function(e) {
		     if(e.deltaY>0) map.zoomOut(e.pageX,e.pageY);
		else if(e.deltaY<0) map.zoomIn(e.pageX,e.pageY);
	});

	var md = function(e) {
		map.mouseDown(e.pageX,e.pageY);
	};
	config.canvas.addEventListener('mousedown',md);
	config.markers.addEventListener('mousedown',md);

	container.addEventListener('mouseup',function(e) {
		map.mouseUp();
	});

	container.addEventListener('mousemove',function(e) {
		map.mouseMove(e.pageX,e.pageY);
	});

	function touchHandler(event) {
		let touch = event.changedTouches[0];
		let simulatedEvent = document.createEvent('MouseEvent');
		simulatedEvent.initMouseEvent({
			touchstart: 'mousedown',
			touchmove: 'mousemove',
			touchend: 'mouseup'
		}[event.type],true,true,global,1,
			touch.screenX,touch.screenY,
			touch.clientX,touch.clientY,
			false,false,false,false,0,null);
		simulatedEvent.touches = event.touches;
		touch.target.dispatchEvent(simulatedEvent);
		event.preventDefault();
	}

	container.addEventListener('touchstart',touchHandler,true);
	container.addEventListener('touchmove',touchHandler,true);
	container.addEventListener('touchend',touchHandler,true);

	config.toggleTerrain.addEventListener('click',function(e) {
		if(map.mode=='terrain') return;
		config.toggleTerrain.setAttribute('class','selected');
		config.toggleTopo.setAttribute('class','');
		config.toggleIso.setAttribute('class','');
		map.mode = 'terrain';
		map.load();
		map.update();
	});

	config.toggleTopo.addEventListener('click',function(e) {
		if(map.mode=='topographic') return;
		config.toggleTerrain.setAttribute('class','');
		config.toggleTopo.setAttribute('class','selected');
		config.toggleIso.setAttribute('class','');
		map.mode = 'topographic';
		map.load();
		map.update();
	});

	config.toggleIso.addEventListener('click',function(e) {
		if(map.mode=='isometric') return;
		config.toggleTerrain.setAttribute('class','');
		config.toggleTopo.setAttribute('class','');
		config.toggleIso.setAttribute('class','selected');
		map.mode = 'isometric';
		map.load();
		map.update();
	});

	config.searchbox.addEventListener('keyup',function(e) {
		if(e.keyCode==13) map.searchDeed();
		else map.autocomplete();
	});

	config.searchbutton.addEventListener('click',function(e) {
		map.searchDeed();
	});

	config.zoomIn.addEventListener('click',function(e) {
		map.zoomIn();
	});

	config.zoomOut.addEventListener('click',function(e) {
		map.zoomOut();
	});

	config.timestamp.innerHTML = getDate(timestamp);
	config.map = map;

})(typeof window === 'undefined' ? this : window);
