package com.wupa9.h2api;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

public class CorsResponseFilter implements ContainerResponseFilter {

	@Override
	public void filter(ContainerRequestContext reqCtx, ContainerResponseContext resCtx) throws IOException {
		resCtx.getHeaders().add("Access-Control-Allow-Origin","*");
        resCtx.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT");
        resCtx.getHeaders().add("Access-Control-Allow-Headers", "Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");
	}

}
