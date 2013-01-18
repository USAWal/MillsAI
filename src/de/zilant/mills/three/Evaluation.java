package de.zilant.mills.three;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

public class Evaluation {
	
	public Evaluation(Rules rules) {
		this.rules = rules;
		positions = new EnumMap<PositionState, Set<Long>>(PositionState.class);
		positions.put(PositionState.WIN,          new TreeSet<Long>());
		positions.put(PositionState.ONLY_TO_WIN,  new TreeSet<Long>());
		positions.put(PositionState.TO_WIN,       new TreeSet<Long>());
		positions.put(PositionState.DRAW,         new TreeSet<Long>());
		positions.put(PositionState.TO_LOSS,      new TreeSet<Long>());
		positions.put(PositionState.ONLY_TO_LOSS, new TreeSet<Long>());
		positions.put(PositionState.LOSS,         new TreeSet<Long>());
		
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Evaluation was started.");
		
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Evaluate:\twins and losses.");
		fillWinsDrawsLosses();
		
		Set<Long> losses3x3 = new HashSet<Long>(positions.get(PositionState.LOSS));
		
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Evaluate:\tonly to losses.");
		fillOnlyToLosses();
		
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Evaluate:\talso to losses");
		fillToLosses();
		
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Evaluate:\tonly to wins.");
		fillOnlyToWins();

		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Evaluate:\talso to wins.");
		fillToWins();

		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Start minimax evaluation.");
		minimaxPositions = new TreeMap<Long, PositionState>();
	    startMinimaxWith(0, true);
	    startMinimaxWith(0, false);
		for(Long position : minimaxPositions.keySet())
			positions.get(minimaxPositions.get(position)).add(position);
		positions.get(PositionState.LOSS).removeAll(losses3x3);
		
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Evaluation is just finished.");
	}
	
	public Map<PositionState, Set<Long>> getPositions() { return positions; }
	
	public static void main (String ... args) {
		try {
			Rules threeMensMorrisRules = new ThreeMensMorrisRules();
			Rules tapatanRules         = new TapatanRules();
			Data data = new Data("tmp/database", threeMensMorrisRules);
			try {
				data.clean();
				data.initialize();
				data.addPosition(new Evaluation(threeMensMorrisRules).getPositions());
				data.release();
				data = new Data("tmp/database", tapatanRules);
				data.addPosition(new Evaluation(    tapatanRules    ).getPositions());
			} finally {
				data.release();
			}
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}
	
	private long increase(long line) {
		long mask = 2;
		while((line & mask) != 0) {
			line -= mask;
			mask <<= 2;
		}
		line += mask >> 1;
		return line;
	}
	
	private void fillWinsDrawsLosses() {
		for(long topLine = 0; topLine <= 0x2A; topLine = increase(topLine))
			for(long bottomLine = topLine; bottomLine <= 0x2A; bottomLine = increase(bottomLine))
				for(long centerLne = 0; centerLne <= 0x2A; centerLne = increase(centerLne)) {
					long position = topLine << 12 | centerLne << 6 | bottomLine;
					if( rules.howManyPiecesOf(position, PieceType.MINE) == 3 && rules.howManyPiecesOf(position, PieceType.OPPONENTS) == 3) {					
						PieceType blocked = rules.whoIsBlocked(position);
						PieceType milled = rules.whoHasAMill(position);
						long symmetricPosition = bottomLine << 12 | centerLne << 6 | topLine;
						if(blocked== PieceType.OPPONENTS || milled == PieceType.MINE) {
							positions.get(PositionState.WIN).add(position);
							positions.get(PositionState.WIN).add(symmetricPosition);
						} else if(blocked == PieceType.MINE || milled == PieceType.OPPONENTS) {
							positions.get(PositionState.LOSS).add(position);
							positions.get(PositionState.LOSS).add(symmetricPosition);
						} else if(milled != PieceType.BOTH) {
							positions.get(PositionState.DRAW).add(position);
							positions.get(PositionState.DRAW).add(symmetricPosition);
						}
					}
				}		
	}
	
	private void fillOnlyToLosses() {
		Collection<Long> newLosses = getReachablePositions(PieceType.OPPONENTS, positions.get(PositionState.DRAW), positions.get(PositionState.LOSS));
		positions.get(PositionState.DRAW).removeAll(newLosses);
		positions.get(PositionState.ONLY_TO_LOSS).addAll(newLosses);
				
		while(!newLosses.isEmpty()) {
			Set<Long> onlyToLosses = new HashSet<Long>();
			for(Long position : getReachablePositions(PieceType.MINE, positions.get(PositionState.DRAW), newLosses))
				if(!isReachableBy(position, positions.get(PositionState.WIN), PieceType.MINE) && !isReachableBy(position, positions.get(PositionState.DRAW), PieceType.MINE))
					onlyToLosses.add(position);
			newLosses = getReachablePositions(PieceType.OPPONENTS, positions.get(PositionState.DRAW), onlyToLosses);
			positions.get(PositionState.DRAW).removeAll(newLosses);
			positions.get(PositionState.ONLY_TO_LOSS).addAll(newLosses);
		}		
	}
	
	private void fillToLosses() {
		Collection<Long> newLosses = positions.get(PositionState.ONLY_TO_LOSS);
		boolean prolonge = false;
		do {
			Set<Long> onlyToLosses = new HashSet<Long>();
			Set<Long> losses = new HashSet<Long>(positions.get(PositionState.ONLY_TO_LOSS));
			losses.addAll(positions.get(PositionState.TO_LOSS));
			for(Long position : getReachablePositions(PieceType.MINE, losses, losses))
				if(!isReachableBy(position, positions.get(PositionState.WIN), PieceType.MINE) && !isReachableBy(position, positions.get(PositionState.DRAW), PieceType.MINE))
					onlyToLosses.add(position);
			newLosses = getReachablePositions(PieceType.OPPONENTS, positions.get(PositionState.DRAW), onlyToLosses);
			positions.get(PositionState.DRAW).removeAll(newLosses);
			positions.get(PositionState.TO_LOSS).addAll(newLosses);
			prolonge = !newLosses.isEmpty();
			
			losses = new HashSet<Long>(positions.get(PositionState.ONLY_TO_LOSS));
			losses.addAll(positions.get(PositionState.TO_LOSS));
			for(Long position : getReachablePositions(PieceType.MINE, positions.get(PositionState.DRAW), newLosses))
				if(!isReachableBy(position, positions.get(PositionState.WIN), PieceType.MINE) && !isReachableBy(position, positions.get(PositionState.DRAW), PieceType.MINE))
					onlyToLosses.add(position);
			newLosses = getReachablePositions(PieceType.OPPONENTS, positions.get(PositionState.DRAW), onlyToLosses);
			positions.get(PositionState.DRAW).removeAll(newLosses);
			positions.get(PositionState.TO_LOSS).addAll(newLosses);
		} while(!newLosses.isEmpty() || prolonge);		
	}
	
	private void fillOnlyToWins() {
		Collection<Long> toWins = null;
		Set<Long> onlyToWins = null;
		
		do {
			toWins = getReachablePositions(PieceType.MINE, positions.get(PositionState.DRAW), positions.get(PositionState.WIN), positions.get(PositionState.ONLY_TO_WIN));
			positions.get(PositionState.DRAW).removeAll(toWins);
			
			Collection<Long> filteredLosses = getReachablePositions(PieceType.MINE, positions.get(PositionState.TO_LOSS), positions.get(PositionState.WIN), positions.get(PositionState.ONLY_TO_WIN));
			positions.get(PositionState.TO_LOSS).removeAll(filteredLosses);
			Collection<Long> filteredOlnlyToLosses = getReachablePositions(PieceType.MINE, positions.get(PositionState.ONLY_TO_LOSS), positions.get(PositionState.WIN), positions.get(PositionState.ONLY_TO_WIN));
			positions.get(PositionState.ONLY_TO_LOSS).removeAll(filteredOlnlyToLosses);
			
			onlyToWins = new HashSet<Long>();
			
			for(Long win : getReachablePositions(PieceType.OPPONENTS, toWins, toWins))
				if(!isReachableBy(win, positions.get(PositionState.TO_LOSS), PieceType.OPPONENTS) && !isReachableBy(win, positions.get(PositionState.ONLY_TO_LOSS), PieceType.OPPONENTS) && !isReachableBy(win, positions.get(PositionState.DRAW), PieceType.OPPONENTS))
					onlyToWins.add(win);
			
			
			for(Long draw : positions.get(PositionState.DRAW))
				if((isReachableBy(draw, toWins, PieceType.OPPONENTS) || isReachableBy(draw, filteredLosses, PieceType.OPPONENTS) || isReachableBy(draw, filteredOlnlyToLosses, PieceType.OPPONENTS)) && !isReachableBy(draw, positions.get(PositionState.TO_LOSS), PieceType.OPPONENTS) && !isReachableBy(draw, positions.get(PositionState.ONLY_TO_LOSS), PieceType.OPPONENTS) && !isReachableBy(draw, positions.get(PositionState.DRAW), PieceType.OPPONENTS))
					onlyToWins.add(draw);
			
			positions.get(PositionState.TO_LOSS).addAll(filteredLosses);
			positions.get(PositionState.ONLY_TO_LOSS).addAll(filteredOlnlyToLosses);
			positions.get(PositionState.DRAW).addAll(toWins);
			positions.get(PositionState.DRAW).removeAll(onlyToWins);
			positions.get(PositionState.ONLY_TO_WIN).addAll(onlyToWins);
		} while(!onlyToWins.isEmpty());		
	}
	
	private void fillToWins() {
		Collection<Long> toWins = getReachablePositions(PieceType.OPPONENTS, positions.get(PositionState.DRAW), getReachablePositions(PieceType.MINE, positions.get(PositionState.DRAW), positions.get(PositionState.WIN), positions.get(PositionState.ONLY_TO_WIN)));
		while(!toWins.isEmpty()) {
			positions.get(PositionState.DRAW).removeAll(toWins);
			positions.get(PositionState.TO_WIN).addAll(toWins);
			toWins = getReachablePositions(PieceType.OPPONENTS, positions.get(PositionState.DRAW), getReachablePositions(PieceType.MINE, positions.get(PositionState.DRAW), toWins));
		}		
	}
	
	private boolean isPieceAdded(Long from, Long to, PieceType type) {
		long difference = from ^ to;
		do {
			if(difference == type.VALUE)    return true;
			else if ((difference & 3) != 0) return false;
			else difference >>= 2;
		} while(difference != 0);	
		return false;
	}
	
	private List<Long> getAppropriateMove(long position, Collection<Long> toes, PieceType type) {
		List<Long> result = new ArrayList<Long>();
		boolean isMiddleStage = rules.howManyPiecesOf(position, PieceType.MINE) == 3 && rules.howManyPiecesOf(position, PieceType.OPPONENTS) == 3;
		for(Long to : toes) {
			if(isMiddleStage) {
				if(rules.isPositionReachableBy(position, to, PieceType.MINE))
					result.add(to);
			} else {
				if(isPieceAdded(position, to, type))
					result.add(to);
			}
		}
		return result;
	}

	private PositionState startMinimaxWith(long position, boolean isMax) {
		PieceType type = isMax ? PieceType.MINE : PieceType.OPPONENTS;
		if(rules.howManyPiecesOf(position, PieceType.MINE) + rules.howManyPiecesOf(position, PieceType.OPPONENTS) > (isMax ? 4 : 5)) {
			
			for(
					int rawState = isMax ? PositionState.WIN.VALUE : PositionState.LOSS.VALUE;
					isMax && rawState >= PositionState.LOSS.VALUE || !isMax && rawState <= PositionState.WIN.VALUE;
					rawState += isMax ? -1 : 1) {
				PositionState positionState = PositionState.getStateOf(rawState);
				Set<Long> filteredPositionsByState = positions.get(positionState);
				if(!filteredPositionsByState.isEmpty()) {
					List<Long> result = getAppropriateMove(position, filteredPositionsByState, type);
					if(!result.isEmpty()) return positionState;
				}
			}
			
			return null;
		}
		PositionState result = null;
		for(long value = type.VALUE << 16; value > 0; value >>= 2) {
			if((position & value) == 0 && (position & (isMax ? value >> 1 : value << 1)) == 0) {
				long variant = position | value;
				PositionState state = PositionState.DRAW;
				PositionState evaluatedState = minimaxPositions.get(variant);
				if(evaluatedState == null) {
					PieceType milled = rules.whoHasAMill(variant);
					PieceType blocked = rules.whoIsBlocked(variant);
					if(blocked== PieceType.MINE || milled == PieceType.OPPONENTS)
						state = PositionState.LOSS;
					else if(blocked== PieceType.OPPONENTS || milled == PieceType.MINE)
						state = PositionState.WIN;
					else
						state = startMinimaxWith(variant, !isMax);
					if(isMax) {
						minimaxPositions.put(variant, state);
						long symmetricValue = (variant & 0x3F000) >> 12 | variant & 0xFC0 | (variant & 0x3F) << 12;
						minimaxPositions.put(symmetricValue, state);
						symmetricValue = (symmetricValue & 0x30C30) >> 4 | symmetricValue & 0xC30C | (symmetricValue & 0x30C3) << 4;
						minimaxPositions.put(symmetricValue, state);
						symmetricValue = (symmetricValue & 0x3F000) >> 12 | symmetricValue & 0xFC0 | (symmetricValue & 0x3F) << 12;
						minimaxPositions.put(symmetricValue, state);
					}
				} else
					state = evaluatedState;
				if(result == null || isMax && state.VALUE > result.VALUE || !isMax && state.VALUE < result.VALUE)
					result = state;
			}
		}
		
		return result;
	}
	
	private Collection<Long> getReachablePositions(PieceType pieceType, Collection<Long> from, Collection<Long> to) {
		Set<Long> result = new HashSet<Long>();
		for(Long position : from)
			if(isReachableBy(position, to, pieceType))
				result.add(position);

		return result;
	}
	
	private Collection<Long> getReachablePositions(PieceType pieceType, Collection<Long> from, Collection<Long> ... toes) {
		Collection<Long> result = new HashSet<Long>();
		for(Collection<Long> to : toes)
			result.addAll(getReachablePositions(pieceType, from, to));
		return result;
	}
	
	private boolean isReachableBy(Long from, Collection<Long> to, PieceType pieceType) {
		for(Long position : to)
			if(rules.isPositionReachableBy(from, position, pieceType))
				return true;
		return false;
	}
	
	private Map<PositionState, Set<Long>> positions;
	private Map<Long, PositionState> minimaxPositions;
	private Rules rules;
}