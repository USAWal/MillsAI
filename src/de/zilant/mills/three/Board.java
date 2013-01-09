package de.zilant.mills.three;

import java.util.Arrays;

import com.sun.org.apache.xpath.internal.axes.HasPositionalPredChecker;


public class Board {
	
	public static Board createBoard(long values, int rawState) {
		int[] result = new int[9];
		for(int index = 0; index < 9; index++) {
			result[index] = (int) values & 3;
			values = values >> 2;
		}
		Board board = new Board(result);
		board.setState(BoardState.defineState(rawState));
		return board;
	}
	
	public Board(int[] values) {
		
		my = new int[values.length];
		opponents = new int[values.length];
		unoccupieds = new int[values.length];
		state = BoardState.DRAW;
		
		int whitesLength = 0;
		int blacksLength = 0;
		int unoccupiedLength = 0;
		
		for(int index = 0; index < values.length; index++) {
			int value = values[index];
			if(value == 2) my[whitesLength++] = index;
			else if(value == 1) opponents[blacksLength++] = index;
			else unoccupieds[unoccupiedLength++] = index;
		}
		
		my = Arrays.copyOf(my, whitesLength);
		opponents = Arrays.copyOf(opponents, blacksLength);
		unoccupieds = Arrays.copyOf(unoccupieds, unoccupiedLength);
		
	}
	
	public long getId() {
		long myPart = 0;
		for(int position : my)
			myPart = myPart | (2 << position*2);
		long opponentsPart = 0;
		for(int position : opponents)
			opponentsPart = opponentsPart | (1 << position*2);
		return myPart | opponentsPart;
	}
	
	public void setState(BoardState state) { this.state = state; }
	public BoardState getState() { return state; }
	
	public int getNumberOf(IntersectionType type) {
		if(type == IntersectionType.OCCUPIED_BY_ME) return my.length;
		else if(type == IntersectionType.OCCUPIED_BY_OPPONENT) return opponents.length;
		else return unoccupieds.length;
	}
	
	public boolean hasMill(IntersectionType type) {
		int[] values = unoccupieds;
		if(type == IntersectionType.OCCUPIED_BY_ME) values = my;
		else if(type == IntersectionType.OCCUPIED_BY_OPPONENT) values = opponents;
		
		if(values.length < 3) return false;
		
		int[] lines = new int[values.length];
		int[] columns = new int[values.length];
		for(int index = 0; index < values.length; index++) {
			lines[index] = values[index]/3;
			columns[index] = values[index]%3;
		}
		
		if(getLongestSequence(lines) >= 3) return true;
		if(getLongestSequence(columns) >= 3) return true;
		
		if(Arrays.binarySearch(values, 4) > -1) {
			if(
					Arrays.binarySearch(values, 0) > -1 && Arrays.binarySearch(values, 8) > -1 ||
					Arrays.binarySearch(values, 2) > -1 && Arrays.binarySearch(values, 6) > -1)
				return true;
		}
		
		return false;
	}
	
	public Board putTo(int position, IntersectionType type) {
		if(type == IntersectionType.UNOCCUPIED) return null;
		int[] positions = unoccupieds;
		if(type == IntersectionType.OCCUPIED_BY_ME) positions = my;
		else if(type == IntersectionType.OCCUPIED_BY_OPPONENT) positions = opponents;
		if(positions.length >= 3) return null;
		if(Arrays.binarySearch(unoccupieds, position) < 0) return null;
		long id = getId();
		id = id & ~(3 << position*2);
		id = id | (type.rawValue << position*2);
		return createBoard(id, 0);
	} 
	
	public static boolean isReachable(Board from, Board to, boolean byOpponent) {
		
		int[] fromA = byOpponent ? from.my : from.opponents;
		int[] toA = byOpponent ? to.my : to.opponents;
		int[] fromB = byOpponent ? from.opponents : from.my;
		int[] toB = byOpponent ? to.opponents : to.my;
		
		if(!Arrays.equals(fromA, toA)) return false;
		if(Arrays.equals(fromB, toB)) return false;
		if(fromA.length != toA.length || fromB.length != toB.length) return false;
		
		int movementStart = -1; 
		for(int fromPosition : fromB)
			if(!hasValue(fromPosition, toB)) {
				if(movementStart > -1) return false;
				else movementStart = fromPosition;
			}
		if(movementStart == 4) return true;
		int startline = movementStart/3;
		int startColumn = movementStart%3;
		for(int toPosition : toB)
			if(!hasValue(toPosition, fromB))
				return
						toPosition == 4 ||
						(startline == toPosition/3 && Math.abs(startColumn- toPosition%3) == 1) ||
						(startColumn == toPosition%3 && Math.abs(startline - toPosition/3) == 1);
		
		return false;
	}
	
	private static boolean hasValue(int checkable, int[] positions) {
		for(int position : positions)
			if(position == checkable) return true;
		return false;
	}
	
	private int getLongestSequence(int[] values) {
		int result = 0;
		int sequenceLong = 0;
		int sequenceValue = 0;
		for(int value : values)
			if(value == sequenceValue) { 
				sequenceLong++; 
			} else {
				if(sequenceLong > result)
					result = sequenceLong;
				sequenceValue = value;
				sequenceLong = 1;
			}
		return result > sequenceLong ? result : sequenceLong;
	}
	
	@Override
	public String toString() {
		return "my is [" + Arrays.toString(my) + "], opponents is [" + Arrays.toString(opponents) + "], state is [" + state + "]";
	}
	
	private int[] my;
	private int[] opponents;
	private int[] unoccupieds;
	private BoardState state;
	
}
