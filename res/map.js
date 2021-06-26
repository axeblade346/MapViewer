

(function(global) {

    const serverInfo = getServerInfo();

    const zoomLevels = [ 0.25, 0.375, 0.5, 0.75, 1.0, 1.25, 1.5, 2, 3, 4, 6, 8 ];
    const zoomScales = [ '25%', '37.5%', '50%', '75%', '100%', '125%', '150%', '200%', '300%', '400%', '600%', '800%' ];

    function getServerInfo() {
        let info = '<h4>'+config.neutralLandName+'</h4>'+
            'Wurm Unlimited Server'+
            '<dl>'+
            '<dt>Kingdoms:</dt>';
        for(let i=1; i<=20; ++i) {
            if(kingdoms[i]===undefined) break;
            info += '<dd>'+kingdoms[i].name+'</dd>';
        }
        info += '<dt>Deeds: '+deeds.length+'</dt>'+
            '<dt>Guard towers: '+guardTowers.length+'</dt>'+
            '</dl>';
        return info;
    }

    function getFocusZoneInfo(map,focusZone) {
        if(focusZone.infoText===undefined) {
            let deeds = 0;
            for(let i=0; i<map.deeds.length; ++i) {
                let d = map.deeds[i];
                if(d.sx<=focusZone.ex && d.ex>=focusZone.sx &&
                   d.sy<=focusZone.ey && d.ey>=focusZone.sy) ++deeds;
            }
            let guardTowers = 0;
            for(let i=0; i<map.guardTowers.length; ++i) {
                let gt = map.guardTowers[i];
                if(gt.x<=focusZone.ex && gt.x>=focusZone.sx &&
                   gt.y<=focusZone.ey && gt.y>=focusZone.sy) ++guardTowers;
            }
            focusZone.infoText = '<h4>'+focusZone.name+'</h4>'+
                                 '<dl>'+
                                 '<dt>Type: '+map.focusZoneTypes[focusZone.type]+'</dt>'+
                                 '<dt>Deeds: '+deeds+'</dt>'+
                                 '<dt>Guard towers: '+guardTowers+'</dt>'+
                                 '</dl>';
        }
        return focusZone.infoText;
    }

    function getDeedInfo(map,deed) {
        if(deed.infoText===undefined) {
            let type = deed.permanent? 'permanent deed' : 'deed';
            let kingdom = kingdoms[deed.kingdom];
            deed.infoText = '<h4>'+deed.name+'</h4>'+
                            '<dl>'+
                            '<dt>Type: '+type+'</dt>'+
                            '<dt>Mayor: <i>'+deed.mayor+'</i></dt>'+
                            '<dt>Kingdom: <i>'+kingdom.name+'</i></dt>'+
                            '<dt>Founded: '+getDate(deed.creationDate)+'</dt>'+
                            (deed.mayor!==deed.founder? '<dt>Founder: <i>'+deed.founder+'</i></dt>' : '')+
                            '<dt>Token: '+deed.x+', '+deed.y+'</dt>'+
                            '<dt>Size: '+(deed.ex-deed.sx+1)+' x '+(deed.ey-deed.sy+1)+' tiles</dt>'+
                            '</dl>';
        }
        return deed.infoText;
    }

    function getGuardTowerInfo(map,guardTower) {
        if(guardTower.infoText===undefined) {
            guardTower.infoText = '<h4>Guard Tower</h4>'+
                                  '<dl>'+
                                  '<dt>Creator: '+guardTower.owner+'</dt>'+
                                  (guardTower.description? '<dt>Description: <i>'+guardTower.description+'</i></dt>' : '')+
                                  '<dt>Location: '+guardTower.x+', '+guardTower.y+'</dt>'+
                                  '</dl>';
        }
        return guardTower.infoText;
    }

    function getSignInfo(map,sign) {
        if(sign.infoText===undefined) {
            sign.infoText = '<h4>Sign</h4>'+
                            '<dl>'+
                            '<dt>Creator: '+sign.owner+'</dt>'+
                            '<dt>Message:</dt><dd class="sign-info">'+sign.message+'</dd>'+
                            '<dt>Location: '+sign.x+', '+sign.y+'</dt>'+
                            '</dl>';
        }
        return sign.infoText;
    }

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

    function toggleClass(element,cl) {
        if(element.classList) element.classList.toggle(cl);
        else {
            var classes = element.className.split(" ");
            var i = classes.indexOf(cl);
            if(i===-1) element.className = classes.push(cl).join(" ");
            else element.className = classes.splice(i, 1).join(" ");
        }
    }

    function addClass(element,cl) {
        if(element.classList) element.classList.add(cl);
        else {
            var classes = element.className.split(" ");
            var i = classes.indexOf(cl);
            if(i===-1) element.className = classes.push(cl).join(" ");
        }
    }

    function removeClass(element,cl) {
        if(element.classList) element.classList.remove(cl);
        else {
            var classes = element.className.split(" ");
            var i = classes.indexOf(cl);
            if(i>=0) element.className = classes.splice(i, 1).join(" ");
        }
    }

    function stopEvent(e) {
        if(e.preventDefault!==undefined) e.preventDefault();
        else if(e.stopPropagation!==undefined) e.stopPropagation();
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

    function Map(config,kingdoms,deeds,focusZones,guardTowers,signs) {
        this.config      = config;
        this.mapImage    = null;
        this.kingdoms    = kingdoms;
        this.deeds       = deeds;
        this.focusDeed        = null;
        this.focusZones  = focusZones;
        this.guardTowers = guardTowers;
        this.signs       = signs;
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
        this.showKingdoms     = false;
        this.showHighways     = false;
        this.showSigns        = false;
        this.markers = [];
        this.pointer = new Marker('pointer',-1,-1,0,null);

        this.config.canvas.setAttribute('width',this.config.size);
        this.config.canvas.setAttribute('height',this.config.size);
        this.config.ctx.imageSmoothingEnabled = false;
        this.config.info.innerHTML = serverInfo;

        this.updateHash = function() {
            let map = this;
            let hash = [];
            let p = this.pointer;
            let x = Math.round(this.x/this.zoom);
            let y = Math.round(this.y/this.zoom);
            hash.push('coords='+x+','+y);
            if(p.x!==-1 && p.y!==-1) hash.push('pointer='+p.x+','+p.y);
            else if(this.focusDeed!==null) hash.push('deed='+this.focusDeed.search);
            if(this.zoomIndex!==4) hash.push('z='+this.zoomIndex);
            let i = 0,l = 0;
            for(let key in this.config.layers) {
                if(map[key]) l |= 1<<i;
                ++i;
            }
            if(l>0) hash.push('l='+l);
            if(hash.length>0 || global.location.hash) global.location.hash = hash.join('&');
        }

        this.updateMarker = function(marker) {
            let x = ((marker.x+0.5)*this.zoom)-128;
            let y = ((marker.y+0.5)*this.zoom)-128;
            if(this.mode==='isometric') y -= marker.height*(this.zoom/40);
            x = Math.round(x);
            y = Math.round(y);
            if(this.showDeeds===false && marker.type==='deed') marker.element.style.display = 'none';
            else marker.element.setAttribute('style','display: block; top: '+y+'px; left: '+x+'px;');
            if(marker.border!==undefined) {
                let w = Math.round(((1+marker.deed.ex-marker.deed.sx)*this.zoom)-2);
                let h = Math.round(((1+marker.deed.ey-marker.deed.sy)*this.zoom)-2);
                let l = Math.floor(128+((marker.deed.sx-marker.deed.x-0.5)*this.zoom));
                let t = Math.floor(128+((marker.deed.sy-marker.deed.y-0.5)*this.zoom));
                let p = (marker.deed.p+5)*this.zoom;
                if(marker.label) marker.label.setAttribute('style','top: '+(y+t)+'px; left: '+(x+l)+'px;');
                if(marker.border) marker.border.setAttribute('style','top: '+t+'px;  left: '+l+'px; width: '+w+'px; height: '+h+'px;');
                if(marker.perimeter) marker.perimeter.setAttribute('style','top: '+(y+t-p)+'px;  left: '+(x+l-p)+'px; width: '+(w+p+p)+'px; height: '+(h+p+p)+'px;display: '+(this.showPerimeters? 'block' : 'none')+';');
                if(marker.bounds) marker.bounds.setAttribute('style','top: '+(y+t)+'px;  left: '+(x+l)+'px; width: '+w+'px; height: '+h+'px;');
            }
        }

        this.updatePointer = function() {
            if(this.pointer.x!==-1 && this.pointer.y!==-1) {
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
                    if(this.mode==='isometric') y -= guardTower.z*(this.zoom/40);
                    guardTower.element.setAttribute('style','display: block; top: '+y+'px; left: '+x+'px;');
                }
            }
            for(let i=0; i<this.signs.length; ++i) {
                let sign = this.signs[i];
                if(this.showSigns===false) sign.element.style.display = 'none';
                else {
                    let x = Math.round(((sign.x+0.5)*this.zoom)-11);
                    let y = Math.round(((sign.y+0.5)*this.zoom)-22);
                    if(this.mode==='isometric') y -= sign.z*(this.zoom/40);
                    sign.element.setAttribute('style','display: block; top: '+y+'px; left: '+x+'px;');
                }
            }
            this.updatePointer();
        }

        this.createFocusZoneBorder = function(marker,focusZone) {
            if((this.mode==='terrain' && !this.config.showDeedBordersInFlatMode) ||
                (this.mode==='topographic' && !this.config.showDeedBordersInFlatMode) ||
                (this.mode==='isometric' && !this.config.showDeedBordersIn3dMode)) return false;
            marker.border = document.createElement('div');
            marker.border.setAttribute('class','border fzb_'+focusZone.type);
            marker.border.focusZone = focusZone;
//			marker.border.setAttribute('title', focusZone.name);
            let map = this;
            marker.border.addEventListener('mouseover',function(e) {
                map.config.info.innerHTML = getFocusZoneInfo(map,focusZone);
            });
            marker.border.addEventListener('mouseout',function(e) {
                map.config.info.innerHTML = serverInfo;
            });
            marker.deed = focusZone;
            return true;
        }

        this.createDeedBounds = function(marker,deed) {
            marker.perimeter = document.createElement('div');
            marker.perimeter.setAttribute('class','perimeter');
            marker.perimeter.deed = deed;
            marker.bounds = document.createElement('div');
            marker.bounds.setAttribute('class','bounds');
            marker.bounds.deed = deed;
            let map = this;
            marker.bounds.addEventListener('mouseover',function(e) {
                map.config.info.innerHTML = getDeedInfo(map,deed);
            });
            marker.bounds.addEventListener('mouseout',function(e) {
                map.config.info.innerHTML = serverInfo;
            });
            return true;
        }

        this.createGuardTower = function(element,guardTower) {
            element.guardTower = guardTower;
            let map = this;
            element.addEventListener('mouseover',function(e) {
                map.config.info.innerHTML = getGuardTowerInfo(map,guardTower);
            });
            element.addEventListener('mouseout',function(e) {
                map.config.info.innerHTML = serverInfo;
            });
        }

        this.createSign = function(element,sign) {
            element.sign = sign;
            let map = this;
            element.addEventListener('mouseover',function(e) {
                map.config.info.innerHTML = getSignInfo(map,sign);
            });
            element.addEventListener('mouseout',function(e) {
                map.config.info.innerHTML = serverInfo;
            });
        }

        this.draw = function() {
            if(this.mapImage===null || this.mapImage.img===null) return;
            let ctx = this.config.ctx;
            ctx.drawImage(this.mapImage.img,0,0);
            this.config.mapFile.href = this.mapImage.url;
            if(this.showKingdoms)
                this.drawKingdoms();
            if(this.showHighways) {
                this.drawHighwayNodes(highwayNodes,"rgba(255,255,0,0.4)","#cc0000","#cc6600");
                this.drawHighwayNodes(bridgeNodes,"rgba(255,153,255,0.4)","#cc0000","#cc6600");
                this.drawHighwayNodes(tunnelNodes,"rgba(0,255,255,0.4)","#cc0000","#cc6600");
            }
        }

        this.drawKingdoms = function() {
            let canvas = document.createElement("canvas");
            let temp = canvas.getContext('2d');
            canvas.width = this.mapImage.img.width;
            canvas.height = this.mapImage.img.height;
            let ctx = this.config.ctx;
            let w = 121;
            let h = 121;
            for(let k=1; k<=20; ++k) {
                let kingdom = this.kingdoms[k];
                if(kingdom===undefined) break;
                temp.fillStyle = kingdom.color;
                temp.beginPath();
                for(let i=0; i<this.guardTowers.length; ++i) {
                    let guardTower = this.guardTowers[i];
                    if(guardTower.kingdom!==k) continue;
                    let x = guardTower.x-60;
                    let y = guardTower.y-60;
                    if(this.mode==='isometric') y -= guardTower.z*(1.0/40);
                    temp.rect(x,y,w,h);
                }
                temp.fill();
            }
            ctx.globalAlpha = 0.3;
            ctx.drawImage(canvas,0,0);
            ctx.globalAlpha = 1.0;
        }

        this.drawHighwayNodes = function(nodes,highwayColor,waystoneBorder,waystoneColor) {
            let ctx = this.config.ctx;
            let z = this.mode==='isometric'? 1/40 : 0;
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
                if(n.length===4 || n.length===7) {
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

        this.mouseToTileX = function(mx) {
            return Math.floor((this.x+mx-(getWidth()/2))/this.zoom);
        }

        this.mouseToTileY = function(my) {
            return Math.floor((this.y+my-(getHeight()/2))/this.zoom);
        }

        this.touchDown = function(mx,my) {
            let element = global.document.elementFromPoint(mx,my);
            let infoText = serverInfo;
            if(element) {
                if(element.focusZone) infoText = getFocusZoneInfo(map,element.focusZone);
                else if(element.deed) infoText = getDeedInfo(map,element.deed);
                else if(element.guardTower) infoText = getGuardTowerInfo(map,element.guardTower);
                else if(element.sign) infoText = getSignInfo(map,element.sign);
            }
            map.config.info.innerHTML = infoText;
        }

        this.mouseDown = function(mx,my,button,touch) {
            if(button===0) {
                this.config.list.style.display = 'none';
                this.mx = mx;
                this.my = my;
                this.md = true;
                this.mm = false;
            }
        }

        this.mouseMove = function(mx,my) {
            if(!this.md) {
                mx = this.mouseToTileX(mx);
                my = this.mouseToTileY(my);
                this.config.coordsMouse.innerHTML = mx+', '+my;
                if(this.pointer.x!==-1 && this.pointer.y!==-1) {
                    let dx = Math.abs(this.pointer.x-mx);
                    let dy = Math.abs(this.pointer.y-my);
                    let d = dx===0? dy : (dy===0? dx : Math.round(Math.sqrt(dx*dx+dy*dy)));
                    this.config.coordsDistance.innerHTML = d+' ['+dx+', '+dy+']';
                }
                return false;
            }
            let dx = mx-this.mx;
            let dy = my-this.my;
            this.mx = mx;
            this.my = my;
            this.mm = true;
            this.go((this.x-dx)/this.zoom,(this.y-dy)/this.zoom);
            return true;
        }

        this.mouseUp = function(button,touch) {
            if(button===0) {
                if(this.md===false) return false;
                this.md = false;
                if(this.mm===false) {
                    let px = this.mouseToTileX(this.mx);
                    let py = this.mouseToTileY(this.my);
                    if((px===this.pointer.x && py===this.pointer.y) || (touch && this.pointer.x!== -1 && this.pointer.y!== -1)) {
                        px = -1;
                        py = -1;
                    }
                    this.setPointer(px,py);
                }
            } else if(button===2) {
                this.setPointer(-1,-1);
            }
            this.updateHash();
            return true;
        }

        this.setPointer = function(px,py) {
            this.leaveDeed();
            this.pointer.x = px;
            this.pointer.y = py;
            if(px!==-1 && py!==-1) {
                addClass(this.config.coords,'pointer-set');
                this.config.coordsPointer.innerHTML = this.pointer.x+', '+this.pointer.y;
                this.config.coordsDistance.innerHTML = '0 [0, 0]';
                let deed = this.getDeed(px,py);
                if(deed) this.setFocusDeed(deed);
                this.updatePointer();
            } else {
                removeClass(this.config.coords,'pointer-set');
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
            if(Math.round(this.zoom)!==this.zoom) style += ' image-rendering: auto; -ms-interpolation-mode: auto;';
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
            if(this.zoomIndex===zoomLevels.length-1) return;
            this.zoomIndex++;
            this.zoomUpdate(mx,my);
            this.updateHash();
        }

        this.zoomOut = function(mx,my) {
            if(this.zoomIndex===0) return;
            this.zoomIndex--;
            this.zoomUpdate(mx,my);
            this.updateHash();
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
            if(text==='') {
                this.config.list.setAttribute('style','display: none;');
                return;
            }
            this.config.list.setAttribute('style','display: block;');
            let html = '';
            for(let i=0; i<this.deeds.length; ++i)
                if(this.deeds[i].search.indexOf(text)===0)
                    html += '<div onclick="config.map.findDeed('+i+');">'+this.deeds[i].name+'</div>';
            for(let i=0; i<this.deeds.length; ++i)
                if(this.deeds[i].search.indexOf(text)>=1)
                    html += '<div onclick="config.map.findDeed('+i+');">'+this.deeds[i].name+'</div>';
            this.config.list.innerHTML = html;
        }

        this.getDeed = function(x,y) {
            for(let i=0; i<this.deeds.length; ++i)  {
                let d = this.deeds[i];
                if(x>=d.sx && y>=d.sy && x<=d.ex && y<=d.ey) return d;
            }
            return null;
        }

        this.searchDeed = function() {
            let text = this.config.searchbox.value.replace(/[^a-zA-Z]/g,'').toLowerCase();
            this.config.list.setAttribute('style','display: none;');
            for(let i=0; i<this.deeds.length; ++i)
                if(this.deeds[i].search.indexOf(text)===0) {
                    this.findDeed(i);
                    return;
                }
            for(let i=0; i<this.deeds.length; ++i)
                if(this.deeds[i].search.indexOf(text)>=1) {
                    this.findDeed(i);
                    return;
                }
        }

        this.getDeedIndex = function(d) {
            if(typeof d === 'object' || typeof d === 'string') {
                for(let i=0; i<this.deeds.length; ++i)
                    if(this.deeds[i]===d || this.deeds[i].search===d) return i;
            } else {
                d = d*1;
                if(d>=0 && d<this.deeds.length) return d;
            }
            return false;
        }

        this.findDeed = function(d) {
            this.gotoDeed(d);
            this.updateHash();
        }

        this.leaveDeed = function() {
            if(this.focusDeed) {
                removeClass(this.focusDeed.marker.border,'selected');
                this.focusDeed = null;
            }
        }

        this.gotoDeed = function(d) {
            this.config.list.setAttribute('style','display: none;');
            this.config.searchbox.value = '';
            this.leaveDeed();
            this.setPointer(-1,-1);
            if(this.setFocusDeed(d))
                this.go(this.focusDeed.x,this.focusDeed.y);
        }

        this.setFocusDeed = function(d) {
            d = this.getDeedIndex(d);
            if(d===false) return false;
            this.focusDeed = this.deeds[d];
            addClass(this.focusDeed.marker.border,'selected');
            return true;
        }

        this.updateLayer = function(key) {
            let layer = this.config.layers[key];
            if(!layer) return;
            let map = this;
            layer.checked = map[key];
            layer.updateLayer = function() {
                map[key] = map.config.layers[key].checked;
                map.updateHash();
                if(key==='showDeeds' || key==='showPerimeters' ||
                   key==='showGuardTowers' || key==='showSigns') map.updateMarkers();
                else if(key==='showKingdoms' || key==='showHighways') map.draw();
                if(key==='showGuardTowers' && map.showGuardTowers && map.zoomIndex<=3)
                    map.addNotification('Guard towers will only show when zooming in more.');
                if(key==='showSigns' && map.showSigns && map.zoomIndex<=7)
                    map.addNotification('Signs will only show when zooming in more.');
            }
            layer.addEventListener('change',layer.updateLayer);
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
                if((this.mode==='terrain' && this.config.showDeedBordersInFlatMode) ||
                    (this.mode==='topographic' && this.config.showDeedBordersInFlatMode) ||
                    (this.mode==='isometric' && this.config.showDeedBordersIn3dMode)) {
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
            for(let i=0; i<this.focusZones.length; ++i) {
                let focusZone = this.focusZones[i];
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
                if(this.createDeedBounds(marker,deed)) {
                    this.config.markers.appendChild(marker.perimeter);
                    this.config.markers.appendChild(marker.bounds);
                }
                deed.search = deed.name.replace(/[^a-zA-Z]/g,'').toLowerCase();
            }
            for(let i=0; i<this.guardTowers.length; ++i) {
                let guardTower = this.guardTowers[i];
                let element = document.createElement('div');
                element.setAttribute('class','tower');
                this.createGuardTower(element,guardTower);
                guardTower.element = element;
                this.config.markers.appendChild(element);
            }
            for(let i=0; i<this.signs.length; ++i) {
                let sign = this.signs[i];
                let element = document.createElement('div');
                element.setAttribute('class','sign');
                this.createSign(element,sign);
                sign.element = element;
                this.config.markers.appendChild(element);
            }
            this.updateMarkers();
            for(let key in this.config.layers)
                this.updateLayer(key);

            let element = document.createElement('div');
            element.className = 'pointer';
            element.style.display = 'none';
            this.config.markers.appendChild(element);
            this.pointer.element = element;
        }

        this.addNotification = function(message) {
            let element = document.createElement('div');
            element.setAttribute('class','notification');
            element.innerHTML = message;
            this.config.container.appendChild(element);
            setTimeout(function() {
                element.remove();
            },3000)
        }

        this.load();
        this.update();

        if(global.location.hash) {
            let params = parseQueryString(global.location.hash.substr(1));
            if(params.z) {
                let z = params.z*1;
                if(z>=0 && z<zoomLevels.length) {
                    this.zoomIndex = z;
                    this.zoomUpdate();
                }
            }
            if(params.deed) {
                this.gotoDeed(params.deed);
            } else if(params.coords || params.pointer) {
                if(params.pointer) {
                    let c = params.pointer.split(',');
                    let x = c[0]*1.0;
                    let y = c[1]*1.0;
                    this.setPointer(x,y);
                    if(!params.coords) this.go(x,y);
                }
                if(params.coords) {
                    let c = params.coords.split(',');
                    let x = c[0]*1.0;
                    let y = c[1]*1.0;
                    this.go(x,y);
                }
            }
            if(params.l) {
                let l = params.l*1;
                let i = 0;
                for(let key in this.config.layers) {
                    let layer = this.config.layers[key];
                    layer.checked = ((1<<(i++))&l)!==0;
                    layer.updateLayer();
                }
            }
        }
    }

    config.container       = document.getElementById('container');
    config.canvas          = document.getElementById('map');
    config.ctx             = config.canvas.getContext('2d');
    config.coords          = document.getElementById('coords');
    config.coordsMouse     = document.getElementById('coords-mouse');
    config.coordsPointer   = document.getElementById('coords-pointer');
    config.coordsDistance  = document.getElementById('coords-distance');
    config.markers         = document.getElementById('markers');
    config.sidebar         = document.getElementById('sidebar');
    config.zoomIn          = document.getElementById('zoom-in');
    config.zoomOut         = document.getElementById('zoom-out');
    config.zoomScale       = document.getElementById('zoom-scale');
    config.toggleTerrain   = document.getElementById('map-terrain');
    config.toggleTopo      = document.getElementById('map-topographic');
    config.toggleIso       = document.getElementById('map-isometric');
    config.layers          = {
        showDeeds:        document.getElementById('layer-deeds'),
        showPerimeters:   document.getElementById('layer-perimeters'),
        showGuardTowers:  document.getElementById('layer-guardtowers'),
        showKingdoms:     document.getElementById('layer-kingdoms'),
        showHighways:     document.getElementById('layer-highways'),
        showSigns:        document.getElementById('layer-signs')
    };
    config.info            = document.getElementById('info');
    config.toggleSidebar   = document.getElementById('sidebar-toggle');
    config.searchbox       = document.getElementById('searchbox');
    config.searchbutton    = document.getElementById('searchbutton');
    config.list            = document.getElementById('autocomplete');
    config.mapFile         = document.getElementById('map-file');
    config.timestamp       = document.getElementById('timestamp');

    var map = new Map(config,kingdoms,deeds,focusZones,guardTowers,signs);

    if(window.matchMedia("(any-hover: none)").matches) {
        addClass(config.container,'touch-display');
        addClass(config.container,'no-sidebar');
    }
    removeClass(config.container,'no-ui');

    config.container.addEventListener('wheel',function(e) {
        if(e.deltaY>0) map.zoomOut(e.pageX,e.pageY);
        else if(e.deltaY<0) map.zoomIn(e.pageX,e.pageY);
        stopEvent(e);
    });

    function mouseDown(e) {
        map.mouseDown(e.pageX,e.pageY,e.button,false);
        stopEvent(e);
    }
    config.canvas.addEventListener('mousedown',mouseDown);
    config.markers.addEventListener('mousedown',mouseDown);
    config.container.addEventListener('mousemove',function(e) {
        if(map.mouseMove(e.pageX,e.pageY)) stopEvent(e);
    });
    config.container.addEventListener('mouseup',function(e) {
        if(map.mouseUp(e.button,false)) stopEvent(e);
    });
    config.canvas.addEventListener('contextmenu',function(e) { stopEvent(e); });
    config.markers.addEventListener('contextmenu',function(e) { stopEvent(e); });

    function touchStart(e) {
        let touch = e.changedTouches[0];
        map.touchDown(touch.clientX,touch.clientY);
        map.mouseDown(touch.clientX,touch.clientY,0,true);
    }
    config.canvas.addEventListener('touchstart',touchStart);
    config.markers.addEventListener('touchstart',touchStart);
    config.container.addEventListener('touchmove',function(e) {
        let touch = e.changedTouches[0];
        if(map.mouseMove(touch.clientX,touch.clientY)) stopEvent(e);
    });
    config.container.addEventListener('touchend',function(e) {
        if(map.mouseUp(0,true)) stopEvent(e);
    });

    config.toggleTerrain.addEventListener('click',function(e) {
        if(map.mode==='terrain') return;
        config.toggleTerrain.setAttribute('class','selected');
        config.toggleTopo.setAttribute('class','');
        config.toggleIso.setAttribute('class','');
        map.mode = 'terrain';
        map.load();
        map.update();
    });

    config.toggleTopo.addEventListener('click',function(e) {
        if(map.mode==='topographic') return;
        config.toggleTerrain.setAttribute('class','');
        config.toggleTopo.setAttribute('class','selected');
        config.toggleIso.setAttribute('class','');
        map.mode = 'topographic';
        map.load();
        map.update();
    });

    config.toggleIso.addEventListener('click',function(e) {
        if(map.mode==='isometric') return;
        config.toggleTerrain.setAttribute('class','');
        config.toggleTopo.setAttribute('class','');
        config.toggleIso.setAttribute('class','selected');
        map.mode = 'isometric';
        map.load();
        map.update();
    });

    config.toggleSidebar.addEventListener('click',function(e) {
        toggleClass(config.container,'no-sidebar');
    });

    config.searchbox.addEventListener('keyup',function(e) {
        if(e.key==='Enter' || e.keyCode===13) map.searchDeed();
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
