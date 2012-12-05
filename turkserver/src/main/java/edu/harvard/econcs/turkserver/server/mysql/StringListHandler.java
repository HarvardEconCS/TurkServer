package edu.harvard.econcs.turkserver.server.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.dbutils.handlers.AbstractListHandler;

@Deprecated
public class StringListHandler extends AbstractListHandler<String> {

	private final String idKey;
	
	public StringListHandler(String idKey) {
		this.idKey = idKey;
	}
	
	@Override
	protected String handleRow(ResultSet rs) throws SQLException {
		return rs.getString(idKey);		
	}

}
