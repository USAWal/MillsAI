package de.zilant.mills.three;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteJob;
import com.almworks.sqlite4java.SQLiteQueue;
import com.almworks.sqlite4java.SQLiteStatement;

public class Data {
	
	public Data() throws Exception
	{
		dbQueue = new SQLiteQueue(new File(DATABASE_PATH))
		{
			@Override
			protected long getReincarnationTimeout() { return DEFAULT_REINCARNATE_TIMEOUT; }
			
			public static final long DEFAULT_REINCARNATE_TIMEOUT = 1000;
		};
		dbQueue.start();
		try
		{
			dbQueue.execute(new SQLiteJob<Object>()
			{
				@Override
				protected Boolean job(SQLiteConnection connection) throws Throwable
				{
					connection
						.exec(BEGIN_TRANSACTION)
						.exec(CREATE_BOARD_TABLE)
						.exec(END_TRANSACTION);
					insert = connection.prepare(INSERT_BOARD);
					selectByState = connection.prepare(SELECT_BOARDS_BY_STATE);
					return null;
				}
			}).get(DATABASE_INTIALIZATION_TIMEOUT, TimeUnit.MILLISECONDS);
		}
		catch(Exception e)
		{
			dbQueue.stop(true);
			throw new Exception(e);
		}
	}
	
	public void addBoard(final Set<Board> boards) {
		dbQueue.execute(new SQLiteJob<Object>() {
			
			@Override
			protected Object job(SQLiteConnection connection) throws Throwable {
				connection.exec(BEGIN_TRANSACTION);
				try {
					for(Board board : boards) {
						int state = 0;
						if(board.getState() == BoardState.LOSS) state = 1;
						else if(board.getState() == BoardState.WIN) state = 2;
						insert.reset().bind(1, board.getId()).bind(2, state).step();
					}
				} finally {
					connection.exec(END_TRANSACTION);
				}
				return null;
			}
			
		});
	}
	
	public List<Board> getBoardsByState(final BoardState state) {
		try {
			return dbQueue.execute(new SQLiteJob<List<Board>>() {
				
				@Override
				protected List<Board> job(SQLiteConnection connection)
						throws Throwable {
					selectByState.reset().bind(1, state.rawValue);
					List<Board> result = new ArrayList<Board>();
					while(selectByState.step())
						result.add(Board.createBoard(
								selectByState.columnLong(0),
								selectByState.columnInt(1)));
					
					return result;
				}
				
			}).get();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void release() {
		try {
			try {
				dbQueue.stop(true).join();
			} finally {
				insert.dispose();
				selectByState.dispose();		
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private SQLiteQueue dbQueue;
	private SQLiteStatement insert;
	private SQLiteStatement selectByState;
	
	private static final String DATABASE_PATH = "tmp/database";
	
	private static final String BEGIN_TRANSACTION = "BEGIN DEFERRED TRANSACTION;";
	private static final String END_TRANSACTION = "END TRANSACTION;";
	
	private static final String CREATE_BOARD_TABLE = "CREATE TABLE IF NOT EXISTS boards (" +
			"id INTEGER PRIMARY KEY NOT NULL, " +
			"state INTEGER NOT NULL " +
			");";
	private static final String INSERT_BOARD = "INSERT INTO boards (id, state) VALUES (:id, :state);";
	private static final String SELECT_BOARDS_BY_STATE = "SELECT * FROM boards WHERE state = :state;";
	
	private static final long DATABASE_INTIALIZATION_TIMEOUT = 1500;
	
}
