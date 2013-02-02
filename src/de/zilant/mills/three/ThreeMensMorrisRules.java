package de.zilant.mills.three;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class ThreeMensMorrisRules implements Rules {
	
	public ThreeMensMorrisRules() {
		connections = new int[][] {
				{0, 1, 3      },
				{0, 0, 2, 4   },
				{0, 1, 5      },
				
				{0, 0, 4, 6   },
				{0, 1, 3, 5, 7},
				{0, 2, 4, 8   },
				
				{0, 3, 7      },
				{0, 4, 6, 8   },
				{0, 5, 7      },
		};
	}
	
	@Override
	public int whatsTheMaxOfPieces() { return 3; }
	
	@Override
	public List<Map<PositionState, Set<Long>>> getPositionsTree() {
		if(positionsTree != null) return positionsTree;
		positionsTree = new ArrayList<Map<PositionState,Set<Long>>>();
		for(int index = 0; index < (whatsTheMaxOfPieces()-2)*(whatsTheMaxOfPieces()-2); index++)
			positionsTree.add(createPositions());
		
		for(long topLine = 0; topLine <= 0x2A; topLine = increaseLine(topLine))
			for(long bottomLine = topLine; bottomLine <= 0x2A; bottomLine = increaseLine(bottomLine))
				for(long centerLne = 0; centerLne <= 0x2A; centerLne = increaseLine(centerLne)) {
					long position = topLine << 12 | centerLne << 6 | bottomLine;
					int myPiecesNumber = howManyPiecesOf(position, PieceType.MINE);
					int opponentsPiecesNumber = howManyPiecesOf(position, PieceType.OPPONENTS);
					if(
							myPiecesNumber        >= 3 && myPiecesNumber        <= whatsTheMaxOfPieces() &&
							opponentsPiecesNumber >= 3 && opponentsPiecesNumber <= whatsTheMaxOfPieces()) {					
						PieceType blocked = whoIsBlocked(position);
						PieceType milled = whoHasAMill(position);
						Map<PositionState, Set<Long>> positions = positionsTree.get((whatsTheMaxOfPieces()-myPiecesNumber)*(whatsTheMaxOfPieces()-2)+(whatsTheMaxOfPieces()-opponentsPiecesNumber));
						long symmetricPosition = bottomLine << 12 | centerLne << 6 | topLine;
						if(blocked== PieceType.OPPONENTS    || (milled == PieceType.MINE      && opponentsPiecesNumber == 3)) {
							positions.get(PositionState.WIN).add(position);
							positions.get(PositionState.WIN).add(symmetricPosition);
						} else if(blocked == PieceType.MINE || (milled == PieceType.OPPONENTS && myPiecesNumber        == 3)) {
							positions.get(PositionState.LOSS).add(position);
							positions.get(PositionState.LOSS).add(symmetricPosition);
						} else if(milled != PieceType.BOTH) {
							positions.get(PositionState.DRAW).add(position);
							positions.get(PositionState.DRAW).add(symmetricPosition);
						}
					}
				}	
		
		return positionsTree;
	}
	
	@Override
	public int whatsTheCode() { return 1; }

	@Override
	public PieceType whoIsBlocked(long position) {
		long blockedPosition         = 0x16620;
		long inversedBlockedPosition = 0x29910;
		if      (position                                  == blockedPosition)         return PieceType.OPPONENTS;
		else if (position                                  == inversedBlockedPosition) return PieceType.MINE;
		else if((position = reflectHorizontally(position)) == blockedPosition)         return PieceType.OPPONENTS;
		else if (position                                  == inversedBlockedPosition) return PieceType.MINE;
		else if((position = reflectVertically(position))   == blockedPosition)         return PieceType.OPPONENTS;
		else if (position                                  == inversedBlockedPosition) return PieceType.MINE;
		else if((position = reflectHorizontally(position)) == blockedPosition)         return PieceType.OPPONENTS;
		else if (position                                  == inversedBlockedPosition) return PieceType.MINE;
		else                                                                           return PieceType.NONE;
	}

	@Override
	public PieceType whoHasAMill(long position) {
		PieceType result     = PieceType.NONE;
		long      lineMask   = 0x0015;
		long      columnMask = 0x1041;
		
		for(int index = 0; index < 3; index++) {
			if((position &  lineMask         ) == lineMask)   if(result == PieceType.NONE) result = PieceType.OPPONENTS; else return PieceType.BOTH;
			if((position & (lineMask   <<= 1)) == lineMask)   if(result == PieceType.NONE) result = PieceType.MINE;      else return PieceType.BOTH;
			if((position &  columnMask       ) == columnMask) if(result == PieceType.NONE) result = PieceType.OPPONENTS; else return PieceType.BOTH;
			if((position & (columnMask <<= 1)) == columnMask) if(result == PieceType.NONE) result = PieceType.MINE;      else return PieceType.BOTH;
			lineMask   <<= 5;
			columnMask <<= 1;
		}
		
		return result;
	}

	@Override
	public int howManyPiecesOf(long position, PieceType pieceType) {
		int result = 0;
		do {
			if((position & 3) == pieceType.VALUE) result++;
			position >>= 2;
		} while(position != 0);

		return result;
	}

	@Override
	public boolean isPositionReachableBy(long from, long to, PieceType typeOfMovingPiece) {
		long differences = from ^ to;
		int fromIndex    = -1;
		int toIndex      = -1;
		int placeIndex   =  0;
		do {
			int positionPieceType = (int) differences & 3;
			if(positionPieceType == typeOfMovingPiece.VALUE) {
				if(fromIndex < 0) fromIndex = placeIndex;
				else if(toIndex < 0) toIndex = placeIndex;
				else return false;
			} else if(positionPieceType != 0)
				return false;
			placeIndex++;
			differences >>= 2;
		} while(differences != 0);
		
		if(	   fromIndex/3 == toIndex/3 && Math.abs(fromIndex%3 - toIndex%3) == 1
			|| fromIndex%3 == toIndex%3 && Math.abs(fromIndex/3 - toIndex/3) == 1)
			return true;
		
		return false;
	}
	
	@Override
	public Collection<Long> getReachablePositionsBy(long position, PieceType pieceType) {
		Collection<Long> result = new TreeSet<Long>();
		fillConnections(position);
		for(int intersectionIndex = 0; intersectionIndex < connections.length; intersectionIndex++) {
			if(connections[intersectionIndex][0] == pieceType.VALUE) {
				long positionWithoutPiece = ~(3 << intersectionIndex*2) & position;
				for(int neighbourIndex = 1; neighbourIndex < connections[intersectionIndex].length; neighbourIndex++) {
					int connectedIntersection = connections[intersectionIndex][neighbourIndex];
					if(connections[connectedIntersection][0] == PieceType.NONE.VALUE)
						result.add(positionWithoutPiece | pieceType.VALUE << 2*connectedIntersection);
				}
			}
		}
		return result;
	}

	@Override
	public long reflectHorizontally(long position) {
		return (position & 0x3F000) >> 12 | position & 0xFC0  | (position & 0x3F)   << 12;
	}

	@Override
	public long reflectVertically(long position) {
		return (position & 0x30C30) >>  4 | position & 0xC30C | (position & 0x30C3) <<  4;
	}
	
	private Map<PositionState,Set<Long>> createPositions() {
		Map<PositionState,Set<Long>> positions = new EnumMap<PositionState, Set<Long>>(PositionState.class);
		positions.put(PositionState.WIN,          new TreeSet<Long>());
		positions.put(PositionState.ONLY_TO_WIN,  new TreeSet<Long>());
		positions.put(PositionState.TO_WIN,       new TreeSet<Long>());
		positions.put(PositionState.DRAW,         new TreeSet<Long>());
		positions.put(PositionState.TO_LOSS,      new TreeSet<Long>());
		positions.put(PositionState.ONLY_TO_LOSS, new TreeSet<Long>());
		positions.put(PositionState.LOSS,         new TreeSet<Long>());
		return positions;
	}
	
	private long increaseLine(long line) {
		long mask = 2;
		while((line & mask) != 0) {
			line -= mask;
			mask <<= 2;
		}
		line += mask >> 1;
		return line;
	}
	
	private void fillConnections(long position) {
		for(int[] intersection : connections) {
			intersection[0] = (int) position & 3;
			position >>= 2;
		}
	}

	private List<Map<PositionState, Set<Long>>> positionsTree = null;
	
	protected int[][] connections;
	
}
