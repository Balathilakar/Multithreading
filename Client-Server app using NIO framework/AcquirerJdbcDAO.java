package com.amfam.billing.acquirer.dataaccess.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.tm.TransactionManagerLocator;
import org.jboss.tm.TxUtils;

import com.amfam.billing.acquirer.AddressPort;
import com.amfam.billing.acquirer.dataaccess.businesstobusiness.ProtocolServiceException;

/**
 * Data Access Object for querying constants needed for acquirer communication.
 * 
 */
public class AcquirerJdbcDAO {

	private static final Log log =
		LogFactory.getLog(AcquirerJdbcDAO.class);

	protected static String GET_DIVISION_NUMBER_SQL = null;
	protected static String GET_CONNECTIONS_SQL = null;
	protected static String GET_CACHE_CONFIG_SQL = null;
	
	private static int stratusDivNum = 0;
	
	private DataSource dataSource;

	/**
	 * Constructor for AcquirerJdbcDAO.
	 */
	public AcquirerJdbcDAO() throws NamingException {
		super();
		init();
	}

	public AcquirerJdbcDAO(DataSource dataSource) throws NamingException
	{
		this.dataSource = dataSource;
		init();
	}
	
	/**
	 * Initializes static members.
	 */
	private void init() throws NamingException {
		// If this dao is configured by dependency injection then the dataSource should already
		// be defined so we do not want to create a new one.
		if (dataSource == null) {
			InitialContext ctx = new InitialContext();
			dataSource = (DataSource)ctx.lookup( "java:comp/env/jdbc/cargroupDS" );
		}

		GET_DIVISION_NUMBER_SQL =
			"SELECT NUMBER_1 FROM "
				+ "PM_PRCINFT where PROCESS_CODE = 'DV'";

		GET_CONNECTIONS_SQL = "select CODE_VALUE_NM from "
							+ "cdvalt where FK_CDTYPT_CODE_ID='ACOM' "
							+ "AND CODE_ID like 'TSY%' "
							+ "order by order_number";
		
		GET_CACHE_CONFIG_SQL = "select CODE_VALUE_NM from "
								+ "cdvalt where FK_CDTYPT_CODE_ID='ACOM' "
								+ "AND CODE_ID ='CACH' ";

	}



	/**
	 * look up the defined connections in the database
	 * @return ArrayList
	 */
	public List<AddressPort> getDefinedConnections(){
		PreparedStatement stmt = null;
		ResultSet rs = null;
	
		Connection conn = null;		
		try
		{

			conn = getConnection();

			stmt = conn.prepareStatement(GET_CONNECTIONS_SQL);
			if (log.isDebugEnabled())
				log.debug("Executing: " + GET_CONNECTIONS_SQL);

			rs = stmt.executeQuery();
			
			ArrayList<AddressPort> list = new ArrayList<AddressPort>();
			while (rs.next()){
				list.add(new AddressPort(rs.getString("CODE_VALUE_NM")));
				
			}
			return list;
		}
		catch (Exception e)
		{
			throw new RuntimeException(
				"Unexpected error getting connection info from db: " + e);
		}
		finally
		{
			close(conn, stmt, rs);
		}

	}
	
	/**
	 * look up parameter for cache config.  If using cache, responses will be cached
	 * and checked based on correlationId
	 * @return boolean
	 */
	public boolean isCacheConfigSettingOn(){
		PreparedStatement stmt = null;
		ResultSet rs = null;
	
		Connection conn = null;		
		try
		{

			conn = getConnection();

			stmt = conn.prepareStatement(GET_CACHE_CONFIG_SQL);
			if (log.isDebugEnabled())
				log.debug("Executing: " + GET_CACHE_CONFIG_SQL);

			rs = stmt.executeQuery();
			String useCache = "";
			if (rs.next()){
				useCache = rs.getString("CODE_VALUE_NM");
			}
				
			//default is to return false.  It must be explicitly set to true to return true
			if (useCache == null || useCache.equals("") ){
				log.info("Config for acquirer was null or empty.  Not using cache");
				return false;
			}
			if (useCache.equalsIgnoreCase("FALSE")){
				log.info("Config for acquirer caching is FALSE explicitly");				
				return false;
			}
			if (useCache.equalsIgnoreCase("TRUE")){
				log.info("Config for acquirer caching is TRUE explicitly");				
				return true;
			}
			
			return false;
		}
		catch (Exception e)
		{
			throw new RuntimeException(
				"Unexpected error getting connection info from db: " + e);
		}
		finally
		{
			close(conn, stmt, rs);
		}

	}
	
	
	/**
	 * Returns the Stratus Divsion Number (int) or zero if it can't find it.
	 * This method only retrieves the Stratus Division Number once.  After that,
	 * it just returns that value each time it is called.
	 * 
	 * @return int Stratus Division Number
	 */
	public int findStratusDivisionNumber() throws ProtocolServiceException
	{

		if (stratusDivNum == 0)
		{
			
			PreparedStatement stmt = null;
			ResultSet rs = null;
	
			Connection conn = null;
	
			try
			{
	
				conn = getConnection();
	
				stmt = conn.prepareStatement(GET_DIVISION_NUMBER_SQL);
				if (log.isDebugEnabled())
					log.debug("Executing: " + GET_DIVISION_NUMBER_SQL);
	
				rs = stmt.executeQuery();
	
				if (rs.next())
				{
					stratusDivNum = rs.getInt(1);
				}
				return stratusDivNum;
	
			}
			catch (Exception e)
			{
				throw new ProtocolServiceException(
					"Unexpected error finding file info: " + e);
			}
			finally
			{
				close(conn, stmt, rs);
			}
		}
		else
		{
			return stratusDivNum;
		}
	}

	/**
	 * @param conn
	 * @param stmt
	 * @param rs
	 */
	private void close(Connection conn, Statement stmt, ResultSet rs) {
		try {
			if (rs != null) rs.close();
		} catch (SQLException ignore) {}
		try {
			if (stmt != null) stmt.close();
		} catch (SQLException ignore) {}
		try {
			if (conn != null) conn.close();
		} catch (SQLException ignore) {}
	}

	private void log(String msg)
	{
		if (log.isDebugEnabled()) log.debug(msg);
	}

	private Connection getConnection() throws Exception {
		Connection conn = dataSource.getConnection();
		TransactionManager tm = TransactionManagerLocator.getInstance().locate();
		if(!TxUtils.isActive(tm)){
			conn.setAutoCommit(false);	
		}
		return conn;
	}

}
