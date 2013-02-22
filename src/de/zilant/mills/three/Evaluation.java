package de.zilant.mills.three;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

public class Evaluation {
	
	public Evaluation(Rules rules) {
		this.rules     = rules;
		minimaxPositions = new TreeMap<Long, PositionState>();
		
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Evaluation was started.");
		this.positions = rules.getPositionsTree().get(rules.getPositionsTree().size() - 1);
		evaluate();
		if(rules.getPositionsTree().size() > 1) {
			for(int index = rules.getPositionsTree().size() - 2; index >= rules.getPositionsTree().size() - rules.whatsTheMaxOfPieces() + 2 - 1; index--) {
				this.positions = rules.getPositionsTree().get(index);
				Map<PositionState, Set<Long>> evaluations = new EnumMap<PositionState, Set<Long>>(PositionState.class);
				deferred_evaluations                      = new EnumMap<PositionState, Set<Long>>(PositionState.class);
				for(long position : positions.get(PositionState.DRAW)) {
					PieceType whoseTheMill = rules.whoHasAMill(position);
					if(whoseTheMill == PieceType.MINE || whoseTheMill == PieceType.BOTH) {
						Collection<Long> reducedPositions = removePiece(PieceType.OPPONENTS, position);
						int state = PositionState.LOSS.VALUE - 1;
						for(long reducedPosition : reducedPositions) {
							minimaxStack.push(reducedPosition);
							int newState = getMin(reducedPosition, rules.getPositionsTree().get(index + 1));							
							minimaxStack.pop();
							if(newState > state)
								state = newState;
						}
						if(state <= PositionState.LOSS.VALUE || state >= PositionState.WIN.VALUE)
							System.out.println("Connected position [" + position + "] evaluated [" + state + "] not in evaluation range");
						else if(state != PositionState.DRAW.VALUE) {
							PositionState pState = PositionState.getStateOf(state);
							Set<Long> evaluatedPositions = evaluations.get(pState);
							if(evaluatedPositions == null) {
								evaluatedPositions = new TreeSet<Long>();
								evaluations.put(pState, evaluatedPositions);
							}
							evaluatedPositions.add(position);
						}
							
					} else if (whoseTheMill == PieceType.OPPONENTS) {
						Collection<Long> reducedPositions = removePiece(PieceType.MINE, position);
						int state = PositionState.WIN.VALUE + 1;
						for(long reducedPosition : reducedPositions) {
							minimaxStack.push(reducedPosition);
							int newState = getMax(reducedPosition, rules.getPositionsTree().get(index + rules.whatsTheMaxOfPieces() - 2));							
							minimaxStack.pop();
							if(newState < state)
								state = newState;
						}
						if(state <= PositionState.LOSS.VALUE || state >= PositionState.WIN.VALUE)
							System.out.println("Connected position [" + position + "] evaluated [" + state + "] not in evaluation range");
						else if(state < PositionState.TO_LOSS.VALUE) {
							PositionState pState = PositionState.getStateOf(state);
							Set<Long> evaluatedPositions = deferred_evaluations.get(pState);
							if(evaluatedPositions == null) {
								evaluatedPositions = new TreeSet<Long>();
								deferred_evaluations.put(pState, evaluatedPositions);
							}
							for(long reachable : rules.getReachablePositionsBy(position, PieceType.OPPONENTS))
								if(rules.whoDidAMill(reachable, position) == PieceType.OPPONENTS)
									evaluatedPositions.add(reachable);
						}
					}
				}
				for(PositionState state : evaluations.keySet()) {
					Set<Long> statedPositions = evaluations.get(state);
					positions.get(PositionState.DRAW).removeAll(statedPositions);
					positions.get(state).addAll(statedPositions);
				}
				evaluate();
				//positions.get(PositionState.LOSS).clear();
			}
		}
		if(!(rules instanceof FiveMensMorrisRules)) {
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Start minimax evaluation.");
	    startMinimaxWith(0, true);
	    startMinimaxWith(0, false);}
	    for(int index = rules.getPositionsTree().size() - 1; index >= rules.getPositionsTree().size() - rules.whatsTheMaxOfPieces() + 2; index--)
	    	rules.getPositionsTree().get(index).get(PositionState.LOSS).clear();
	    if(!(rules instanceof FiveMensMorrisRules))
	    for(Long position : minimaxPositions.keySet())
			positions.get(minimaxPositions.get(position)).add(position);	
		
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Evaluation is just finished.");
	}
	
	private int getMin(long position, Map<PositionState, Set<Long>> positions) {
		int state = PositionState.WIN.VALUE + 1;
		for(long reachable : rules.getReachablePositionsBy(position, PieceType.OPPONENTS)) {
			if(rules.whoIsBlocked(reachable) == PieceType.MINE || positions.get(PositionState.LOSS).contains(reachable) && rules.whoDidAMill(position, reachable) == PieceType.OPPONENTS)
				return PositionState.ONLY_TO_LOSS.VALUE;
			else if(minimaxStack.contains(reachable)) {
				if(PositionState.DRAW.VALUE < state)
					state = PositionState.DRAW.VALUE;
			} else {
				minimaxStack.push(reachable);
				int newState = getMax(reachable, positions);
				minimaxStack.pop();
				if(newState < state)
					state = newState;
			}
		}
		return state;
	}
	
	private int getMax(long position, Map<PositionState, Set<Long>> positions) {
		int state = PositionState.LOSS.VALUE - 1;
		for(long reachable : rules.getReachablePositionsBy(position, PieceType.MINE)) {
			for(int rawState = PositionState.WIN.VALUE; rawState > state; rawState--)
				if(positions.get(PositionState.getStateOf(rawState)).contains(reachable))
					state = rawState;
			if(state == PositionState.LOSS.VALUE) {
				PositionState previouslySavedState = minimaxPositions.get(reachable);
				if(previouslySavedState != null) return previouslySavedState.VALUE;
				else if (minimaxStack.contains(reachable)) state = PositionState.DRAW.VALUE;
				else {
					minimaxStack.push(reachable);
					state = getMin(reachable, positions);
					minimaxStack.pop();
				}
				minimaxPositions.put(reachable, PositionState.getStateOf(state));
			}
		}
		return state;
	}
	
	private Stack<Long> minimaxStack = new Stack<Long>();
	
	private Collection<Long> removePiece(PieceType pieceType, long position) {
		Collection<Long> brokenMill = new TreeSet<Long>();
		Collection<Long> unbrokenMill = new TreeSet<Long>();
		PieceType whoseTheMill = rules.whoHasAMill(position);
		
		for(long mask = pieceType.VALUE; mask < (long) pieceType.VALUE << 2*rules.whatsTheMaxOfPlaces(); mask <<= 2)
			if((mask & position) != 0) {
				long newPosition = ~mask & position;
				(whoseTheMill == rules.whoHasAMill(newPosition) ? unbrokenMill : brokenMill).add(newPosition);
			}
		
		
		return unbrokenMill.isEmpty() ? brokenMill : unbrokenMill;
	}
	
	private Collection<Long> addPiece(PieceType pieceType, Collection<Long> ... positions) {
		Collection<Long> result = new TreeSet<Long>();
		for(Collection<Long> positionCollection : positions)
			for(Long position : positionCollection) {
				long mask = 3;
				for(long value = pieceType.VALUE; value < pieceType.VALUE << 32; value <<= 2) {
					if((position & mask) == 0) result.add(position | value);
					mask <<= 2;
				}
			}
		return result;
	}
	
	private void addDeferredEvaluations(PositionState state) {
			Set<Long> statedPositions = deferred_evaluations.get(state);
			if(statedPositions == null) return;
			statedPositions.retainAll(positions.get(PositionState.DRAW));
			positions.get(PositionState.DRAW).removeAll(statedPositions);
			positions.get(state).addAll(statedPositions);
	}
	
	private void evaluate() {
		addDeferredEvaluations(PositionState.ONLY_TO_LOSS);
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Evaluate:\tonly to losses.");
		fillOnlyToLosses();
		
		addDeferredEvaluations(PositionState.TO_LOSS);
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Evaluate:\talso to losses");
		fillToLosses();
		
		addDeferredEvaluations(PositionState.ONLY_TO_WIN);
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Evaluate:\tonly to wins.");
		fillOnlyToWins();

		addDeferredEvaluations(PositionState.TO_WIN);
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Evaluate:\talso to wins.");
		fillToWins();
		
		if(rules.getPositionsTree().size() > 1) {			
			Map<PositionState, Set<Long>> evaluations = new EnumMap<PositionState, Set<Long>>(PositionState.class);
			for(long position : positions.get(PositionState.LOSS)) {
				if(rules.whoIsBlocked(position) != PieceType.MINE && !positions.get(PositionState.WIN).contains(position)) {
					int state = getMin(position, positions);
					if(state == PositionState.WIN.VALUE)
						state = PositionState.ONLY_TO_WIN.VALUE;
					if(state <= PositionState.LOSS.VALUE || state >= PositionState.WIN.VALUE)
						System.out.println("Connected position [" + position + "] evaluated [" + state + "] not in evaluation range");
					else {
						PositionState pState = PositionState.getStateOf(state);
						Set<Long> evaluatedPositions = evaluations.get(pState);
						if(evaluatedPositions == null) {
							evaluatedPositions = new TreeSet<Long>();
							evaluations.put(pState, evaluatedPositions);
						}
						evaluatedPositions.add(position);
					}
				}
			}
			for(PositionState state : evaluations.keySet()) {
				Set<Long> statedPositions = evaluations.get(state);
				positions.get(PositionState.LOSS).removeAll(statedPositions);
				positions.get(state).addAll(statedPositions);
			}
		}
	}
	
	public Map<PositionState, Set<Long>> getPositions() { return positions; }
	
	public static void main (String ... args) {
		try {
			Rules threeMensMorrisRules = new ThreeMensMorrisRules();
			Rules tapatanRules         = new TapatanRules();
			Rules fiveMensMorrisRules  = new FiveMensMorrisRules();
			Data data = new Data("tmp/database", threeMensMorrisRules);
			try {
				data.clean();
				data.initialize();
				data.addPosition(new Evaluation(threeMensMorrisRules).getPositions());
				data.release();
				data  = new Data("tmp/database",        tapatanRules)                ;
				data.addPosition(new Evaluation(        tapatanRules).getPositions());
				data.release();
				data  = new Data("tmp/database", fiveMensMorrisRules)                ;
				new Evaluation(fiveMensMorrisRules)                                  ;
				//for(Map<PositionState, Set<Long>> positions : fiveMensMorrisRules.getPositionsTree())
				for(int index = 1; index <= 4; index++)
					data.addPosition(fiveMensMorrisRules.getPositionsTree().get(fiveMensMorrisRules.getPositionsTree().size() - index));//	data.addPosition(positions);
			} finally {
				data.release();
			}
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}
	
	private void fillOnlyToLosses() {
		System.out.println("fillOnlyToLosses start");
		Collection<Long> newLosses = new TreeSet<Long>();
		for(long newLoss : getReachablePositions(PieceType.OPPONENTS, positions.get(PositionState.DRAW), positions.get(PositionState.LOSS))) {
			for(long reachable : rules.getReachablePositionsBy(newLoss, PieceType.OPPONENTS)) {
				if(rules.whoIsBlocked(reachable) == PieceType.MINE || rules.whoDidAMill(newLoss, reachable) == PieceType.OPPONENTS) {
					newLosses.add(newLoss);
					break;
				}
			}
		}
		System.out.println("newLosses size is [" + newLosses.size() + "]");
		positions.get(PositionState.DRAW).removeAll(newLosses);
		positions.get(PositionState.ONLY_TO_LOSS).addAll(newLosses);
		newLosses = positions.get(PositionState.ONLY_TO_LOSS);
		
		while(!newLosses.isEmpty()) {
			Set<Long> onlyToLosses = new HashSet<Long>();
			for(Long position : getReachablePositions(PieceType.MINE, positions.get(PositionState.DRAW), newLosses))
				if(
						!isReachableBy(position, positions.get(PositionState.ONLY_TO_WIN), PieceType.MINE)  &&
						!isReachableBy(position, positions.get(PositionState.WIN        ), PieceType.MINE)  &&
						!isReachableBy(position, positions.get(PositionState.DRAW       ), PieceType.MINE))
					onlyToLosses.add(position);
			newLosses = getReachablePositions(PieceType.OPPONENTS, positions.get(PositionState.DRAW), onlyToLosses);
			positions.get(PositionState.DRAW).removeAll(newLosses);
			positions.get(PositionState.ONLY_TO_LOSS).addAll(newLosses);
		}
	}
	
	private void fillToLosses() {
		Collection<Long> newLosses = positions.get(PositionState.ONLY_TO_LOSS);
		Collection<Long> newLosses2 = null;
		boolean prolonge = false;
		do {
			Set<Long> onlyToLosses = new HashSet<Long>();
			Set<Long> losses = new TreeSet<Long>(positions.get(PositionState.ONLY_TO_LOSS));
			losses.addAll(positions.get(PositionState.TO_LOSS));
			for(Long position : getReachablePositions(PieceType.MINE, losses, losses))
				if(!isReachableBy(position, positions.get(PositionState.ONLY_TO_WIN), PieceType.MINE) && !isReachableBy(position, positions.get(PositionState.WIN), PieceType.MINE) && !isReachableBy(position, positions.get(PositionState.DRAW), PieceType.MINE))
					onlyToLosses.add(position);
			newLosses = getReachablePositions(PieceType.OPPONENTS, positions.get(PositionState.DRAW), onlyToLosses);
			positions.get(PositionState.DRAW).removeAll(newLosses);
			prolonge = !newLosses.isEmpty();
			
			for(Long position : getReachablePositions(PieceType.MINE, positions.get(PositionState.DRAW), newLosses))
				if(!isReachableBy(position, positions.get(PositionState.ONLY_TO_WIN), PieceType.MINE) && !isReachableBy(position, positions.get(PositionState.WIN), PieceType.MINE) && !isReachableBy(position, positions.get(PositionState.DRAW), PieceType.MINE))
					onlyToLosses.add(position);
			newLosses2 = getReachablePositions(PieceType.OPPONENTS, positions.get(PositionState.DRAW), onlyToLosses);
			positions.get(PositionState.DRAW).removeAll(newLosses2);
			positions.get(PositionState.TO_LOSS).addAll(newLosses);
			positions.get(PositionState.TO_LOSS).addAll(newLosses2);
		} while(!newLosses2.isEmpty() || prolonge);		
	}
	
	private void fillOnlyToWins() {
		Collection<Long> onlyToWins = null;
		do {
			Collection<Long> toWins = getReachablePositions(PieceType.MINE, positions.get(PositionState.ONLY_TO_LOSS), positions.get(PositionState.WIN), positions.get(PositionState.ONLY_TO_WIN)) ;
			toWins            .addAll(getReachablePositions(PieceType.MINE, positions.get(PositionState.TO_LOSS     ), positions.get(PositionState.WIN), positions.get(PositionState.ONLY_TO_WIN)));
			toWins            .addAll(getReachablePositions(PieceType.MINE, positions.get(PositionState.DRAW        ), positions.get(PositionState.WIN), positions.get(PositionState.ONLY_TO_WIN)));
			toWins            .addAll(getReachablePositions(PieceType.MINE, positions.get(PositionState.ONLY_TO_WIN ), positions.get(PositionState.WIN), positions.get(PositionState.ONLY_TO_WIN)));
			
			onlyToWins = new TreeSet<Long>();
			for(long draw : positions.get(PositionState.DRAW))
				if(toWins.containsAll(rules.getReachablePositionsBy(draw, PieceType.OPPONENTS)))
					onlyToWins.add(draw);
			
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
		System.out.println("from size is [" + from.size() + "]");
		System.out.println("to size is [" + to.size() + "]");
		Set<Long> result = new HashSet<Long>();

		if(from.size() <= to.size()) {
			for(Long position : from)
				if(isReachableBy(position, to, pieceType))
					result.add(position);
		} else {
			for(Long position : to) {
				Collection<Long> reachables = rules.getReachablePositionsBy(position, pieceType);
				reachables.retainAll(from);
				result.addAll(reachables);
			}
		}

		return result;
	}
	
	private Collection<Long> getReachablePositions(PieceType pieceType, Collection<Long> from, Collection<Long> ... toes) {
		Collection<Long> result = new TreeSet<Long>();
		for(Collection<Long> to : toes)
			result.addAll(getReachablePositions(pieceType, from, to));
		return result;
	}
	
	private boolean isReachableBy(Long from, Collection<Long> to, PieceType pieceType) {
		for(Long reachable : rules.getReachablePositionsBy(from, pieceType))
			if(to.contains(reachable))
				return true;
		return false;
	}
	
	private Map<PositionState, Set<Long>> deferred_evaluations = new EnumMap<PositionState, Set<Long>>(PositionState.class);
	private Map<PositionState, Set<Long>> positions;
	private Map<Long, PositionState>      minimaxPositions;
	private Rules                         rules;
	
}
