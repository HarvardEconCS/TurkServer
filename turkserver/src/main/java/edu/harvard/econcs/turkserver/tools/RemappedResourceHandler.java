package edu.harvard.econcs.turkserver.tools;

import java.io.File;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.util.Random;

import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.resource.Resource;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

/**
 * This resourceHandler obfuscates filenames for a given path, and generates
 * random filenames which it then handles with the original files.
 * @author mao
 *
 */
public class RemappedResourceHandler extends ResourceHandler {

	private final Logger LOG = Log.getLogger(this.getClass());
	private Random rnd = new Random();
	
	BiMap<String, String> aliasToActualPath;
	String basePath;
	
	public RemappedResourceHandler() {
		BiMap<String, String> bm = HashBiMap.create();
		aliasToActualPath = Maps.synchronizedBiMap(bm);
	}
	
	public String getRemappedPath(String actualPath) {
		return aliasToActualPath.inverse().get(actualPath);		
	}
	
	@Override
	public void doStart() throws Exception {		
		super.doStart();
		
		// Add all files under the current base resource as random names
		File baseFile = super.getBaseResource().getFile();
		basePath = baseFile.getPath();
		addFileAliases(baseFile, basePath);
	}		

	private void addFileAliases(File file, String basePath) {
		// Recursively add all files in this directory
		
		File[] files = file.listFiles();
		if( files == null ) return;
		
		for( File f : files ) {
			if( f.getName().startsWith(".") )
				continue;
			else if( f.isDirectory() )
				addFileAliases(f, basePath);
			else if ( f.isFile() ) {
				String ext = getExtension(f);
				String alias = "/" + new BigInteger(256, rnd).toString(36) + (ext.isEmpty() ? "" : "." + ext ); 
				String actualPath = f.getPath().replace(basePath, "");
						
				LOG.info("Path {} mapped to alias {}", actualPath, alias);
				aliasToActualPath.put(alias, actualPath);
			}
		}			
		
	}
	
	@Override
	public Resource getResource(String path) throws MalformedURLException {
		
		String actualPath = aliasToActualPath.get(path);
		
		if( actualPath != null ) {
			LOG.info("Retrieving {} for alias {}", actualPath, path);
			return super.getResource(actualPath);
		}
		else {			
			return null;
		}
	}

	public static String getExtension(File f)
	{
		String ext = null;
		String s = f.getName();
		int i = s.lastIndexOf('.');
	
		if (i > 0 && i < s.length() - 1)
			ext = s.substring(i+1).toLowerCase();
	
		if(ext == null)
			return "";
		return ext;
	}

}
