package de.zilant.mills.three;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteJob;
import com.almworks.sqlite4java.SQLiteQueue;
import com.almworks.sqlite4java.SQLiteStatement;

public class Data {
	
	public Data(String path, Rules rules)
	{
		this.rules = rules;
		initialization = new SQLiteJob<Object>() {
				
				@Override
				protected Boolean job(SQLiteConnection connection) throws Throwable
				{
					connection
						.exec(BEGIN_TRANSACTION      )
						.exec( CREATE_POSITIONS_TABLE)
						.exec( CREATE_RULES_TABLE    )
						.exec( CREATE_STATES_TABLE   )
						.exec(END_TRANSACTION        );
					SQLiteStatement statement = connection.prepare(INSERT_RULE).bind(1, Data.this.rules.whatsTheCode());
					statement.step();
					statement.dispose();
					return null;
				}
				
			};
		dbQueue = new SQLiteQueue(new File(path))
		{
			@Override
			protected long getReincarnationTimeout() { return DEFAULT_REINCARNATE_TIMEOUT; }
			
			public static final long DEFAULT_REINCARNATE_TIMEOUT = 1000;
		};
		dbQueue.start();
	}
	
	public void initialize() { dbQueue.execute(initialization); }
	
	public void clean() {
		dbQueue.execute(new SQLiteJob<Object>() {
			@Override
			protected Object job(SQLiteConnection connection) throws Throwable {
				connection
					.exec(BEGIN_TRANSACTION)
					.exec( CLEAN_STATES    )
					.exec( CLEAN_POSITIONS )
					.exec( CLEAN_RULES     )
					.exec(END_TRANSACTION  );
				return null;
			}
		});
	}
	
	public void addPosition(final Map<PositionState, Set<Long>> positions) {
		dbQueue.execute(new SQLiteJob<Object>() {
			@Override
			protected Object job(SQLiteConnection connection) throws Throwable {
				SQLiteStatement insert_position = connection.prepare(INSERT_POSITION);
				SQLiteStatement insert_state    = connection.prepare(INSERT_STATE   );
				connection.exec(BEGIN_TRANSACTION);
				try {
					for(int state = PositionState.WIN.VALUE; state >= PositionState.LOSS.VALUE; state--)
						for(Long value : positions.get(PositionState.getStateOf(state))) {
								insert_position
									.reset(                       )
									.bind (1, value               )
									.step (                       );
								insert_state
									.reset(                       )
								    .bind (1, value               )
								    .bind (2, rules.whatsTheCode())
								    .bind (3, state               )
								    .step (                       );
						}
				} finally {
					connection.exec(END_TRANSACTION);
				}
				insert_position.dispose();
				insert_state   .dispose();
				return null;
			}
			
		});
	}
	
	public List<Long> getPositionsByState(final PositionState state) {
		try {
			return dbQueue.execute(new SQLiteJob<List<Long>>() {
				@Override
				protected List<Long> job(SQLiteConnection connection) throws Throwable {
					List<Long> result = new ArrayList<Long>();
					SQLiteStatement statement = connection.prepare(SELECT_POSITIONS).bind(1, rules.whatsTheCode()).bind(2, state.VALUE);
					while(statement.step())
						result.add(Long.valueOf(statement.columnLong(0)));		
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
			dbQueue.stop(true).join();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private       SQLiteQueue          dbQueue;
	private final Rules                  rules;
	private final SQLiteJob<Object> initialization;
	
	private static final String BEGIN_TRANSACTION              = "BEGIN DEFERRED TRANSACTION;";
	private static final String END_TRANSACTION                = "END            TRANSACTION;";
	
	private static final String CREATE_POSITIONS_TABLE         = "CREATE TABLE IF NOT EXISTS positions (" +
			"id         INTEGER PRIMARY KEY,   " +
			"code       INTEGER NOT NULL UNIQUE" +
			");";
	private static final String CREATE_RULES_TABLE             = "CREATE TABLE IF NOT EXISTS rules     (" +
			"id         INTEGER PRIMARY KEY,   " +
			"code       INTEGER NOT NULL UNIQUE" +
			");";
	private static final String CREATE_STATES_TABLE            = "CREATE TABLE IF NOT EXISTS states    (" +
			"position   INTEGER NOT NULL REFERENCES positions (code) ON DELETE CASCADE, " +
			"rule       INTEGER NOT NULL REFERENCES   rules   (code) ON DELETE CASCADE, " +
			"evaluation INTEGER NOT NULL, " +
			"PRIMARY KEY (position, rule) " +
			");";
	
	private static final String CLEAN_POSITIONS                = "DROP TABLE IF EXISTS positions;";
	private static final String CLEAN_RULES                    = "DROP TABLE IF EXISTS     rules;";
	private static final String CLEAN_STATES                   = "DROP TABLE IF EXISTS    states;";
	
	private static final String INSERT_POSITION                = "INSERT OR IGNORE  INTO positions (           code           ) VALUES (            :code            );";
	private static final String INSERT_RULE                    = "INSERT OR IGNORE  INTO rules     (           code           ) VALUES (            :code            );";
	private static final String INSERT_STATE                   = "INSERT OR REPLACE INTO states    (position, rule, evaluation) VALUES (:position, :rule, :evaluation);";
	
	private static final String SELECT_POSITIONS               = "SELECT position FROM states WHERE rule = :rule AND evaluation = :evaluation;";
	
	private static final long   DATABASE_INTIALIZATION_TIMEOUT = 1500;
	
}
