package de.zilant.mills.three;

public enum BoardState {
	WIN(3),
	ONLY_TO_WIN(2),
	TO_WIN(1),
	DRAW(0),
	TO_LOSS(-1),
	ONLY_TO_LOSS(-2);
	
	public static BoardState defineState(int rawState) {
		switch (rawState) {
		case -2: return ONLY_TO_LOSS;
		case -1: return TO_LOSS;
		case  1: return TO_WIN;
		case  2: return ONLY_TO_WIN;
		case  3: return WIN;
		default: return DRAW;
		}
	}
	
	private BoardState(int rawValue) { this.rawValue = rawValue; }
	
	@Override
	public String toString() { return representations[rawValue + 2]; }
	
	public int rawValue;
	
	private static final String[] representations = new String[] {"ONLY TO LOSS", "TO LOSS", "DRAW", "TO WIN", "ONLY TO WIN", "WIN"};
}
