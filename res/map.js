

(function(global) {

	function getWidth() {
		return global.innerWidth || document.documentElement.clientWidth || document.body.clientWidth;
	}

	function getHeight() {
		return global.innerHeight || document.documentElement.clientHeight || document.body.clientHeight;
	}

	function Section(x,y,url) {
		this.x         = x;
		this.y         = y;
		this.url       = url;
		this.loaded    = false;
		this.attempts  = 0;
		this.img       = null;
	}

	function SectionLoader(map, queue) {
		this.map = map;
		this.queue = queue;
		this.loadAll = function() {
			while(queue.length>0) {
				let section = this.queue.pop();
				this.load(section);
			}
		}
		this.load = function(section) {
			let img = new Image();
			img.section = section;
			img.loader = this;
			img.onload = function() {
				this.section.img = this;
				this.section.loaded = true;
				this.loader.map.signal(this.section);
			}
			let fail = function() {
				this.section.attempts++;
				if(this.section.attempts<4) {
					this.loader.load(this.section);
				}
			}
			img.onerror = fail;
			img.onabort = fail;
			img.src = section.url;
		}
	}

	function Marker(x,y,height,element) {
		this.x        = x;
		this.y        = y;
		this.height   = height;
		this.element  = element;
	}

	function Map(config,deeds,focusZones) {
		this.config      = config;
		this.deeds       = deeds;
		this.focusZones  = focusZones;
		this.mode        = config.mode;
		this.focusZoneTypes = [
			'none',
			'volcano',
			'PvP',
			'name',
			'name',
			'PvE',
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
		this.x     = config.x;
		this.y     = config.y;
		this.zoom  = 1;
		this.mx    = 0;
		this.my    = 0;
		this.md    = false;

		this.config.canvas.setAttribute('width',this.config.dimension);
		this.config.canvas.setAttribute('height',this.config.dimension);
		this.config.ctx.imageSmoothingEnabled = false;

		this.markers = [];

		this.updateMarker = function(marker) {
			let x = ((marker.x+0.5)*this.zoom)-128;
			let y = ((marker.y+0.5)*this.zoom)-128;
			if(this.mode=='3d') y -= marker.height*(this.zoom/40);
			x = Math.round(x);
			y = Math.round(y);
			marker.element.setAttribute('style','top: '+y+'px; left: '+x+'px;');
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

		this.updateMarkers = function() {
			for(let i=0; i<this.markers.length; ++i)
				this.updateMarker(this.markers[i]);
		}

		this.createFocusZoneBorder = function(marker,focusZone) {
			if((this.mode=='3d' && !this.config.showDeedBordersIn3dMode) ||
				(this.mode=='flat' && !this.config.showDeedBordersInFlatMode) ||
				(this.mode=='roads' && !this.config.showDeedBordersInFlatMode)) return false;
			marker.border = document.createElement('div');
			marker.border.setAttribute('class','border fzb_'+focusZone.type);
//			marker.border.setAttribute('title', focusZone.name);
			let map = this;
			let infoText = focusZone.name+' ('+this.focusZoneTypes[focusZone.type]+')';
			marker.border.addEventListener('mouseover',function(e) {
				let info = map.config.info;
				info.innerHTML = infoText;
				info.style.display = 'block';
			});
			marker.border.addEventListener('mouseout',function(e) {
				let info = map.config.info;
				info.style.display = 'none';
			});
			marker.deed = focusZone;
			return true;
		}

		this.createDeedBounds = function(marker,deed) {
			marker.bounds = document.createElement('div');
			marker.bounds.setAttribute('class','bounds');
			let map = this;
			let kingdom = kingdoms[deed.kingdom];
			let infoText = 'Deed: <b>'+deed.name+'</b>, Mayor: <i>'+deed.mayor+'</i>, Kingdom: <i>'+kingdom.name+'</i>';
			marker.bounds.addEventListener('mouseover',function(e) {
				let info = map.config.info;
				info.innerHTML = infoText;
				info.style.display = 'block';
			});
			marker.bounds.addEventListener('mouseout',function(e) {
				let info = map.config.info;
				info.style.display = 'none';
			});
			return true;
		}

		this.signal = function(section) {
			this.config.ctx.drawImage(section.img,section.x*this.config.step,section.y*this.config.step);
		}

		this.go = function(x,y) {
			this.x = x*this.zoom;
			this.y = y*this.zoom;
			this.update();
		}

		this.mouseDown = function(mx,my) {
			this.config.list.setAttribute('style','display: none;');
			this.mx = mx;
			this.my = my;
			this.md = true;
		}

		this.mouseMove = function(mx,my) {
			if(!this.md) {
				this.config.coords.innerHTML = Math.floor((this.x+mx-(getWidth()/2))/this.zoom)+', '+Math.floor((this.y+my-(getHeight()/2))/this.zoom);
				return;
			}
			let dx = mx-this.mx;
			let dy = my-this.my;
			this.mx = mx;
			this.my = my;
			this.go((this.x-dx)/this.zoom,(this.y-dy)/this.zoom);
		}

		this.mouseUp = function() {
			this.md = false;
		}

		this.update = function() {
			this.clamp();
			let width = getWidth();
			let height = getHeight();
			let s = this.config.dimension*this.zoom;
			let style = 'width: '+s+'px; height: '+s+'px; top: '+Math.round((height/2)-this.y)+'px; left: '+Math.round((width/2)-this.x)+'px;';
			this.config.markers.setAttribute('style',style);
			if(this.zoom<0.999) style += ' image-rendering: auto; -ms-interpolation-mode: auto;';
			this.config.canvas.setAttribute('style',style);
		}

		this.clamp = function() {
			if(this.zoom>0.999) this.zoom = Math.round(this.zoom);
			if(this.x<0) this.x = 0;
			if(this.y<0) this.y = 0;
			if(this.x>=this.config.dimension*this.zoom) this.x = this.config.dimension*this.zoom;
			if(this.y>=this.config.dimension*this.zoom) this.y = this.config.dimension*this.zoom;
		}

		this.zoomIn = function(mx,my) {
			if(this.zoom>7.999) return;
			if(mx!==undefined && my!==undefined) {
				this.x += (mx-(getWidth()/2))/2;
				this.y += (my-(getHeight()/2))/2;
			}
			this.zoom *= 2;
			this.x *= 2;
			this.y *= 2;
			this.updateMarkers();
			this.update();
		}

		this.zoomOut = function(mx,my) {
			if(this.zoom<0.249) return;
			if(mx!==undefined && my!==undefined) {
				this.x -= (mx-(getWidth()/2))/2;
				this.y -= (my-(getHeight()/2))/2;
			}
			this.zoom /= 2;
			this.x /= 2;
			this.y /= 2;
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
			this.config.list.innerHTML = '';
			for(let i=0; i<this.deeds.length; ++i) {
				let deed = this.deeds[i];
				let name = deed.name.replace(/[^a-zA-Z]/g,'').toLowerCase();
				if(name.indexOf(text)===0) {
					this.config.list.innerHTML += '<div onclick="javascript:config.map.gotoDeed('+i+');">'+deed.name+'</div>';
				}
			}
		}

		this.search = function() {
			let text = this.config.searchbox.value.replace(/[^a-zA-Z]/g,'').toLowerCase();
			this.config.list.setAttribute('style','display: none;');
			for(let i=0; i<this.deeds.length; ++i) {
				let deed = this.deeds[i];
				let name = deed.name.replace(/[^a-zA-Z]/g,'').toLowerCase();
				if(name.indexOf(text)===0) {
					this.gotoDeed(i);
					return;
				}
			}
		}

		this.gotoDeed = function(i) {
			this.config.list.setAttribute('style','display: none;');
			this.config.searchbox.value = '';
			let deed = this.deeds[i];
			this.go(deed.x,deed.y);
		}

		this.load = function() {
			this.config.markers.innerHTML = '';
			this.markers = [];
			var queue = [];
			for(let x=0; x<this.config.size; ++x)
				for(let y=0; y<this.config.size; ++y)
					queue.push(new Section(x,y,'sections-'+this.mode+'/'+x+'.'+y+'.png'));
			new SectionLoader(this, queue).loadAll();
			for(let i=0; i<this.deeds.length; ++i) {
				let deed = this.deeds[i];
				let name = deed.name;
				if(name.length>24) name = name.substring(0,22)+'...';
				let element = document.createElement('div');
				let marker = new Marker(deed.x,deed.y,deed.height,element);
				deed.marker = marker;
				this.updateMarker(marker);
				if((this.mode=='3d' && this.config.showDeedBordersIn3dMode) ||
					(this.mode=='flat' && this.config.showDeedBordersInFlatMode) ||
					(this.mode=='roads' && this.config.showDeedBordersInFlatMode)) {
					marker.border = document.createElement('div');
					marker.border.setAttribute('class','border');
					marker.deed = deed;
					element.appendChild(marker.border);
				}
				let label = document.createElement('span');
				label.innerHTML = name;
				label.setAttribute('class',deed.permanent? 'deed permanent' : 'deed');
				element.setAttribute('class','marker');
				element.appendChild(label);
				this.config.markers.appendChild(element);
				this.markers.push(marker);
			}
			for(let i=0; i<this.focusZones.length; ++i) {
				let focusZone = this.focusZones[i];
				let name = focusZone.name;
				let element = document.createElement('div');
				let marker = new Marker(focusZone.x,focusZone.y,focusZone.height,element);
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
			}
			this.updateMarkers();
		}

		this.load();
		this.update();
	}

	config.dimension     = config.size * config.step;
	config.container     = document.getElementById('container');
	config.canvas        = document.getElementById('map');
	config.ctx           = config.canvas.getContext('2d');
	config.coords        = document.getElementById('coords');
	config.info          = document.getElementById('info');
	config.timestamp     = document.getElementById('timestamp');
	config.markers       = document.getElementById('markers');
	config.toggle3d      = document.getElementById('3d');
	config.toggleflat    = document.getElementById('flat');
	config.toggleroads   = document.getElementById('roads');
	config.searchbox     = document.getElementById('searchbox');
	config.searchbutton  = document.getElementById('searchbutton');
	config.list          = document.getElementById('autocomplete');
	config.zoomin        = document.getElementById('zoomin');
	config.zoomout       = document.getElementById('zoomout');

	var map = new Map(config, deeds, focusZones);

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

	config.toggle3d.addEventListener('click',function(e) {
		if(map.mode=='3d') return;
		config.toggle3d.setAttribute('class','selected');
		config.toggleflat.setAttribute('class','');
		config.toggleroads.setAttribute('class','');
		map.mode = '3d';
		map.load();
		map.update();
	});

	config.toggleflat.addEventListener('click',function(e) {
		if(map.mode=='flat') return;
		config.toggle3d.setAttribute('class','');
		config.toggleflat.setAttribute('class','selected');
		config.toggleroads.setAttribute('class','');
		map.mode = 'flat';
		map.load();
		map.update();
	});

	config.toggleroads.addEventListener('click',function(e) {
		if(map.mode=='roads') return;
		config.toggle3d.setAttribute('class','');
		config.toggleflat.setAttribute('class','');
		config.toggleroads.setAttribute('class','selected');
		map.mode = 'roads';
		map.load();
		map.update();
	});

	config.searchbox.addEventListener('keyup',function(e) {
		if(e.keyCode==13) map.search();
		else map.autocomplete();
	});

	config.searchbutton.addEventListener('click',function(e) {
		map.search();
	});

	config.zoomin.addEventListener('click',function(e) {
		map.zoomIn();
	});

	config.zoomout.addEventListener('click',function(e) {
		map.zoomOut();
	});

	let date = new Date(timestamp);
	let yy = date.getFullYear();
	let mm = date.getMonth()+1;
	let dd = date.getDate();
	config.timestamp.innerHTML = yy+'-'+(mm<=9? '0' : '')+mm+'-'+(dd<=9? '0' : '')+dd;
	config.map = map;

})(typeof window === 'undefined' ? this : window);


