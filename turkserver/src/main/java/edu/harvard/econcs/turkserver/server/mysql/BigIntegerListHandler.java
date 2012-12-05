package edu.harvard.econcs.turkserver.server.mysql;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.dbutils.handlers.AbstractListHandler;

@Deprecated
public class BigIntegerListHandler extends AbstractListHandler<BigInteger> {

	private final String idKey;
	
	public BigIntegerListHandler(String idKey) {
		this.idKey = idKey;
	}
	
	@Override
	protected BigInteger handleRow(ResultSet rs) throws SQLException {
		return new BigInteger(rs.getString(idKey), 16);		
	}

}
