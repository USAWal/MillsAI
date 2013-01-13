package de.zilant.mills.three;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingWorker;

public class Game extends Component implements MouseListener {
	
	public Game(Data data) throws IOException {
		movingPiecePosition = -1;
		whites = new ArrayList<Point>();
		blacks = new ArrayList<Point>();
		board = new Position(0);
		ai = new Ai(data);
		//aiMove();
	}
	
	public static void main(String... args) {
		JFrame frame = null;
		try {
			final Data data = new Data("tmp/database");
			frame = new JFrame("Mills");
			frame.setPreferredSize(new Dimension(300, 300));
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.addWindowListener(new WindowAdapter() {
				
				@Override
				public void windowClosing(WindowEvent e) {
					if(data != null)
						data.release();
				}
				
			});
			Game game = new Game(data);
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

		int startX = 7*size.width/60;
		int startY = 7*size.height/60;
		int stepX = size.width/3;
		int stepY = size.height/3;
		long id = board.VALUE;
		for(int position = 0; position < 9; position++) {
			int value = 3 & (int) id;
			id = id >> 2;
			graphics.setColor(value == PieceType.OPPONENTS.VALUE ? Color.YELLOW : Color.DARK_GRAY);
			if(value != PieceType.NONE.VALUE)
				graphics.fillOval(startX + (position%3)*stepX, startY + (position/3)*stepY, size.width/10, size.height/10);
		}
	};
	
	private void moveTo(int x, int y) {
		int position = y*3 + x;
		if(
				board.NUMBER_OF_OPPONENTS_PIECES == 3 &&
				board.NUMBER_OF_MY_PIECES == 3) {
			if(movingPiecePosition < 0) {
				if(((board.VALUE >> position*2) & 3) == PieceType.OPPONENTS.VALUE) {
					movingPiecePosition = position;
				}
				return;
			}
			long newBoardId = (board.VALUE & ~(3 << position*2)) | (PieceType.OPPONENTS.VALUE << position*2);
			newBoardId = newBoardId & ~(3 << movingPiecePosition*2);
			Position newBoard = new Position(newBoardId);
			movingPiecePosition = -1;
			if(Position.isReachable(board, newBoard, true))
				board = newBoard;
			else
				return;
		} else {
			Position newBoard = board.putTo(position, PieceType.OPPONENTS);
			if(newBoard != null)
				board = newBoard;
			else
				return;
		}
		repaint();
		aiMove();
	}
	
	private void aiMove() {
		new SwingWorker<Position, Object>() {

			@Override
			protected Position doInBackground() throws Exception {
				return ai.getMove(board, true);
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
		}.execute();		
	}
	
	BufferedImage background;
	List<Point> whites;
	List<Point> blacks;
	Position board;
	Ai ai;
	int movingPiecePosition;
	
	@Override
	public void mouseClicked(MouseEvent e) {
		Dimension size = getSize();
		int startX = 7*size.width/60;
		int startY = 7*size.height/60;
		int stepX = size.width/3;
		int stepY = size.height/3;
		for(int y = 0; y < 3; y ++)
			for(int x = 0; x < 3; x ++)
				if(
						Math.abs(startX + x*stepX - e.getX()) < size.width/10 &&
						Math.abs(startY + y*stepY - e.getY()) < size.height/10)
					moveTo(x, y);
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
