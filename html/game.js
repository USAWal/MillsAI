var SET_STAGE = 0;

function Board(){
  this.pieces = [0, 0, 0, 0, 0, 0, 0, 0, 0];
  this.stage = SET_STAGE;
  this.player_mode = 0;
  this.waits = false;
}

Board.prototype.place_value = function(index) { return this.pieces[index]; }

Board.prototype.turn_light_on = function(piece) {
  if(this.waits) return;
  if(this.place_value(piece.index) == 2) return;
  if(this.stage == SET_STAGE) {
    if(this.place_value(piece.index) == 1) return;
      piece.childNodes[1].childNodes[1].childNodes[3 - this.player_mode*2].setAttribute('opacity', '1.0');
  } else {
    if(this.place_value(piece.index) == 1) {
      piece.childNodes[1].childNodes[1].childNodes[3 - this.player_mode*2].setAttribute('opacity', '0.5');
      piece.childNodes[3].childNodes[3 - this.player_mode*2].setAttribute('opacity', '0.5');
    } else
      piece.childNodes[1].childNodes[1].childNodes[3 - this.player_mode*2].setAttribute('opacity', '1.0');
  }
  piece.childNodes[3].childNodes[3 - this.player_mode*2].setAttribute('opacity', '1.0');
}

Board.prototype.turn_light_off = function(piece) {
  if(this.waits) return;
  if(this.place_value(piece.index) == 2) return;
  if(this.stage == SET_STAGE) {
    if(this.place_value(piece.index) == 1) return;
      piece.childNodes[1].childNodes[1].childNodes[3 - this.player_mode*2].setAttribute('opacity', '0.0');
  } else {
    if(this.place_value(piece.index) == 1) {
      piece.childNodes[1].childNodes[1].childNodes[3 - this.player_mode*2].setAttribute('opacity', '0.0');
      piece.childNodes[3].childNodes[3 - this.player_mode*2].setAttribute('opacity', '0.5');
    } else
      piece.childNodes[1].childNodes[1].childNodes[3 - this.player_mode*2].setAttribute('opacity', '0.0');
  }
  piece.childNodes[3].childNodes[3 - this.player_mode*2].setAttribute('opacity', '0.0');
}

var board = new Board();

function reset_piece(piece, index) {
  piece.index = index;
  for(var player_index = 1; player_index < 5; player_index += 2) {
    piece.childNodes[1].childNodes[1].childNodes[player_index].setAttribute('opacity', '0.0');
    piece.childNodes[3].childNodes[player_index].setAttribute('opacity', '0.0');
  }
}


