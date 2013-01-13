package de.zilant.mills.three;

public enum PositionState {
	WIN          ( 3),
	ONLY_TO_WIN  ( 2),
	TO_WIN       ( 1),
	DRAW         ( 0),
	TO_LOSS      (-1),
	ONLY_TO_LOSS (-2);
	
	@Override
	public String toString() { return descriptions[descriptions.length - (3 + VALUE)]; }
	
	public static PositionState getStateOf(int value) {
		switch (value) {
		case  3: return WIN;
		case  2: return ONLY_TO_WIN;
		case  1: return TO_WIN;
		case -1: return TO_LOSS;
		case -2: return ONLY_TO_LOSS;
		default: return DRAW;
		}
	}
	
	private PositionState(int value) { this.VALUE = value; }
	
	public final int VALUE;
	
	private static final String[] descriptions = new String[] {
		"WIN"         ,
		"ONLY TO WIN" ,
		"TO WIN"      ,
		"DRAW"        ,
		"TO LOSS"     ,
		"ONLY TO LOSS"};
}
