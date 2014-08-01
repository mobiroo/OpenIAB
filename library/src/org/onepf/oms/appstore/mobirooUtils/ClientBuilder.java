package org.onepf.oms.appstore.mobirooUtils;

public class ClientBuilder
{
	private HttpRequest httpRequest;
	private RestClient restClient;
	public ClientBuilder()
	{
		httpRequest = new HttpRequest();
		restClient = new RestClient();
	}
	
	public RestClient build()
	{
		RestClient client = new RestClient(httpRequest, restClient.getConnectionTimeout(), restClient.getSocketTimeout());
		return client;
	}
	
	public ClientBuilder setBaseUri(String baseUri)
	{
		httpRequest.setBaseUri(baseUri);
		return this;
	}
	
	public ClientBuilder setHttpMethod(HttpMethod method)
	{
		httpRequest.setHttpMethod(method);
		return this;
	}
	
	public ClientBuilder setCharSetType(String charSetType)
	{
		httpRequest.setCharSetType(charSetType);
		return this;
	}
	
	public ClientBuilder setContentType(String contentType)
	{
		httpRequest.setContentType(contentType);
		return this;
	}
	
	public ClientBuilder addParam(String key,String value)
	{
		httpRequest.addParam(key, value);
		return this;
	}
	
	public ClientBuilder addHeader(String key,String value)
	{
		httpRequest.addHeader(key, value);
		return this;
	}
	
	public ClientBuilder addPath(String... paths)
	{
		httpRequest.addPath(paths);
		return this;
	}
	
	public ClientBuilder setSocketTimeout(int socketTimeout)
	{
		restClient.setSocketTimeout(socketTimeout);
		return this;
	}
	
	public ClientBuilder setConnectionTimeout(int connectionTimeout)
	{
		restClient.setConnectionTimeout(connectionTimeout);
		return this;
	}
	
	public ClientBuilder setRequestBody(byte[] requestBody)
	{
		httpRequest.setRequestBody(requestBody);
		return this;
	}
	
	public ClientBuilder setRequestBody(String requestBody)
	{
		byte[] body = null;
		try
		{
			body = requestBody.getBytes(httpRequest.getCharSetType());
		}
		catch(Exception ex)
		{
			body = requestBody.getBytes();
		}
		
		httpRequest.setRequestBody(body);
		return this;
	}
}
