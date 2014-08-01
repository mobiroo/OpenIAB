package org.onepf.oms.appstore.mobirooUtils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.apache.http.HttpEntity;


public class HttpEntityHelper
{
	/**
	 * read the response entity as string
	 * @param entity
	 * @return
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public static String readEntityAsString(HttpEntity entity) throws IllegalStateException, IOException 
	{
		InputStream inputStream  = null;
		StringBuilder stringBuilder = new StringBuilder();
		try
		{
			inputStream = entity.getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			String line = null;
			while((line = reader.readLine())!=null)
			{
				stringBuilder.append(line);
			}
		}
		catch(IllegalStateException ex)
		{
			throw ex;
		}
		catch(IOException ex)
		{
			throw ex;
		}
		finally
		{
			try
			{
				if(inputStream!=null) inputStream.close();
			}
			catch(Exception ex)
			{
				//Logger.printStackTrace(ex);
			}
		}
		
		return stringBuilder.toString();
	}
	
	public static byte[] readEntity(HttpEntity entity) throws IOException
	{
		InputStream is = null;
		try
		{
			is = entity.getContent();

			ByteArrayOutputStream buffer = new ByteArrayOutputStream();

			int nRead;
			byte[] data = new byte[16384];

			while ((nRead = is.read(data, 0, data.length)) != -1)
			{
				buffer.write(data, 0, nRead);
			}

			buffer.flush();

			return buffer.toByteArray();
		}
		catch (IllegalStateException e)
		{
			throw e;
		}
		catch (IOException e)
		{
			throw e;
		}
	}
}
