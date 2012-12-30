var SET_STAGE = 0;
var MOVE_STAGE = 1;
var WHITE_PLAYER = 0
var BLACK_PLAYER = 1;

function Board(){
  this.pieces = new Array();
  this.stage = SET_STAGE;
  this.player_mode = WHITE_PLAYER;
  this.waits = false;
}

Board.prototype.bind = function(piece, index) {
  piece.value = 0;
  piece.white_light = piece.childNodes[1].childNodes[1].childNodes[3];
  piece.black_light = piece.childNodes[1].childNodes[1].childNodes[1];
  piece.gold_coin   = piece.childNodes[3].childNodes[3];
  piece.silver_coin = piece.childNodes[3].childNodes[1];
  this.pieces[index] = piece;
  
  piece.white_light.setAttribute('opacity', '0.0');
  piece.black_light.setAttribute('opacity', '0.0');
  piece.gold_coin.setAttribute('opacity', '0.0');
  piece.silver_coin.setAttribute('opacity', '0.0');

  piece.on_mouse_moved = function(isOver) {
    if(board.waits) return;
    if(this.value == 2) return;
    if(board.stage == SET_STAGE && this.value == 1) return;

    var light = (board.player_mode == WHITE_PLAYER ? this.white_light : this.black_light);
    var coin  = (board.player_mode == WHITE_PLAYER ? this.gold_coin : this.silver_coin);

    if(board.stage == MOVE_STAGE && this.value == 1)
      var opacity = isOver ? '0.5' : '1.0';
    else
      var opacity = isOver ? '1.0' : '0.0';

    light.setAttribute('opacity', opacity);
    coin.setAttribute('opacity', opacity);
  }

  piece.onmouseover = function() { this.on_mouse_moved(true); }
  piece.onmouseout = function() { this.on_mouse_moved(false); }
  piece.onclick = function() {
    if(board.waits) return;
    if(board.stage == SET_STAGE && this.value == 0) {
      this.value = 1;
      (board.player_mode == WHITE_PLAYER ? this.white_light : this.black_light).setAttribute('opacity', '0.0');
      (board.player_mode == WHITE_PLAYER ? this.gold_coin : this.silver_coin).setAttribute('opacity', '1.0');
    }
    //else if (this.value == 1) board.select(this);
    //else if (this.value == 0) board.moveTo(this);
  }
}

var board = new Board();

