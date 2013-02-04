package de.zilant.mills.three;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingWorker;

public class Game extends Component implements MouseListener {
	
	public Game(BufferedImage image, Data data, Rules rules) throws Exception {
		this.background = image;
		this.rules = rules;
		movingPiecePosition = -1;
		whites = new ArrayList<Point>();
		blacks = new ArrayList<Point>();
		board = 0;
		this.data = data;
		random = new Random();
		List<Long> positions = data.getPositionsByState(PositionState.ONLY_TO_LOSS);
		board = positions.get(random.nextInt(positions.size()));
		//aiMove();
	}
	
	public static void main(String... args) {
		JFrame frame = null;
		try {
			Rules rules = new FiveMensMorrisRules();
			final Data data = new Data("tmp/database", rules);
			frame = new JFrame("Mills");
			frame.setPreferredSize(new Dimension(600, 600));
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.addWindowListener(new WindowAdapter() {
				
				@Override
				public void windowClosing(WindowEvent e) {
					if(data != null)
						data.release();
				}
				
			});
			Game game = new Game(ImageIO.read(new File("html/Six_Men's_Morris.jpg")), data, rules);
			game.addMouseListener(game);
			frame.add(game, BorderLayout.CENTER);
			frame.pack();
			frame.setVisible(true);
		} catch (Exception e1) {
			e1.printStackTrace();
			System.exit(-1);
		}
	}
	
	public void paint(java.awt.Graphics g) {
		Graphics2D graphics = (Graphics2D) g;
		Dimension size = getSize();
		graphics.drawImage(background, 0, 0, size.width, size.height, null);

		/*int startX = 7*size.width/120 + size.width/20;
		int startY = 7*size.height/120 + size.height/20;
		int stepX = size.width/3;
		int stepY = size.height/3;
		long id = board;
		for(int position = 0; position < 9; position++) {
			int value = 3 & (int) id;
			id = id >> 2;
			graphics.setColor(value == PieceType.OPPONENTS.VALUE ? Color.YELLOW : Color.DARK_GRAY);
			if(value != PieceType.NONE.VALUE)
				graphics.fillOval(startX + (position%3)*stepX, startY + (position/3)*stepY, size.width/10, size.height/10);
		}*/
		long id = board;
		for(int index = 0; index < intersections.length; index++) {
				int value = 3 & (int) id;
				id = id >> 2;
				graphics.setColor(value == PieceType.OPPONENTS.VALUE ? Color.YELLOW : Color.DARK_GRAY);
				if(value != PieceType.NONE.VALUE)
					graphics.fillOval(intersections[index].x - size.width/20, intersections[index].y - size.height/20, size.width/10, size.height/10);
			}
	};
	
	private void moveTo(int palceIndex) {
		System.out.println("Move to [" + palceIndex + "]");
		if(
				rules.howManyPiecesOf(board, PieceType.MINE) == 3 && rules.howManyPiecesOf(board, PieceType.OPPONENTS) == 3) {
			if(movingPiecePosition < 0) {
				if(((board >> palceIndex*2) & 3) == PieceType.OPPONENTS.VALUE) {
					movingPiecePosition = palceIndex;
				}
				return;
			}
			long newBoardId = (board & ~(3 << palceIndex*2)) | (PieceType.OPPONENTS.VALUE << palceIndex*2);
			newBoardId = newBoardId & ~(3 << movingPiecePosition*2);
			long newBoard = newBoardId;
			movingPiecePosition = -1;
			if(rules.isPositionReachableBy(board, newBoard, PieceType.OPPONENTS))
				board = newBoard;
			else {
				System.out.println("Position is not reachable");
				return;
			}
		} else {
			long newBoard = putTo(palceIndex, PieceType.OPPONENTS);
			if(newBoard < 0)
				return;
			else
				board = newBoard;
		}
		repaint();
		aiMove();
	}
	
	private long putTo(int placeIndex, PieceType pieceType) {
		if(pieceType == PieceType.NONE) return -1;
		if(rules.howManyPiecesOf(board, pieceType) >= 3) return -1;
		if((board >> placeIndex * 2 & 3) != PieceType.NONE.VALUE) return -1;
		long positionValue = board & ~(3 << placeIndex*2) | (pieceType.VALUE << placeIndex*2);
		return positionValue;
	} 
	
	private void aiMove() {
		new SwingWorker<Long, Object>() {

			@Override
			protected Long doInBackground() throws Exception {
				for(int rawState = PositionState.WIN.VALUE; rawState >= PositionState.LOSS.VALUE; rawState --) {
					List<Long> boards = data.getPositionsByState(PositionState.getStateOf(rawState));
					if(!boards.isEmpty()) {
						List<Long> result = getAppropriateMove(board, boards, PieceType.MINE);
						if(!result.isEmpty()) return result.get(random.nextInt(result.size()));
					}
				}
				return null;
			}
			
			@Override
			protected void done() {
				try {
					board = get();
					repaint();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			private List<Long> getAppropriateMove(Long board, Collection<Long> boards, PieceType type) {
				List<Long> result = new ArrayList<Long>();
				boolean isMiddleStage = rules.howManyPiecesOf(board, PieceType.MINE) == 3 && rules.howManyPiecesOf(board, PieceType.OPPONENTS) == 3;
				for(Long to : boards) {
					if(isMiddleStage) {
						if(rules.isPositionReachableBy(board, to, PieceType.MINE))
							result.add(to);
					} else {
						if(isPieceAdded(board, to, type))
							result.add(to);
					}
				}
				return result;
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
		}.execute();		
	}
	
	Data data;
	Random random;
	BufferedImage background;
	List<Point> whites;
	List<Point> blacks;
	long board;
	int movingPiecePosition;
	Rules rules;
	Point[] intersections = new Point[] {
			new Point(60, 60), new Point(300, 60), new Point(540, 60),
			new Point(180, 180), new Point(300, 180), new Point(420, 180),
			new Point(60, 300), new Point(180, 300), new Point(420, 300), new Point(540, 300),
			new Point(180, 420), new Point(300, 420), new Point(420, 420),
			new Point(60, 540), new Point(300, 540), new Point(540, 540)
	};
	
	@Override
	public void mouseClicked(MouseEvent e) {
		/*Dimension size = getSize();
		int startX = 7*size.width/120 + size.width/20;
		int startY = 7*size.height/120 + size.height/20;
		int stepX = size.width/3;
		int stepY = size.height/3;
		for(int y = 0; y < 3; y ++)
			for(int x = 0; x < 3; x ++)
				if(
						Math.abs(startX + x*stepX - e.getX()) < size.width/10 &&
						Math.abs(startY + y*stepY - e.getY()) < size.height/10)
					moveTo(x, y);
		}*/
		
		for(int index = 0; index < intersections.length; index++)
				if(e.getPoint().distance(intersections[index]) < getSize().width/10)
					moveTo(index);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
}
