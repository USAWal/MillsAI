package de.zilant.mills.three;

public enum BoardState {
	WIN(2),
	LOSS(1),
	DRAW(0);
	
	private BoardState(int rawValue) { this.rawValue = rawValue; }
	
	@Override
	public String toString() { return representations[rawValue]; }
	
	public int rawValue;
	
	private static final String[] representations = new String[] {"DRAW", "LOSS", "WIN"};
}
