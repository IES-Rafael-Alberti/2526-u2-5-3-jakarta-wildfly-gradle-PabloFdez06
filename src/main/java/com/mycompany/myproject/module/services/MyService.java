package com.mycompany.myproject.module.services;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.mycompany.myproject.module.Pojo;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.enterprise.context.ApplicationScoped;


@ApplicationScoped // <--- AÑADE ESTO para que persista
@Path("myservice")
public class MyService {
	private final Map<Integer, Pojo> store = new ConcurrentHashMap<>();
	private final AtomicInteger sequence = new AtomicInteger(2);

	@PostConstruct
	void init() {
		store.put(1, new Pojo(1, "LALALA"));
		store.put(2, new Pojo(2, "LElele"));
	}

	@GET
	@Path("/hello")
	public Response sayHello(@Context HttpServletRequest request) {
		Response response = Response.ok("hello!").build();
		return response;
	}

	@GET
	@Path("/pojo/list")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<Pojo> getAll() {
		return new ArrayList<>(store.values());
	}

	@GET
	@Path("/pojo/find/{id}")
	@Produces({ MediaType.APPLICATION_JSON })
	public Pojo find(@PathParam("id") Integer id) {
		Pojo pojo = store.get(id);
		if (pojo == null) {
			throw new jakarta.ws.rs.NotFoundException("Pojo not found: " + id);
		}
		return pojo;
	}

	@POST
	@Path("/pojo/new")
	@Consumes({ MediaType.APPLICATION_JSON })
	public Response create(Pojo pojo) {

		if (pojo == null) {
			return Response.status(400).entity("Body required").build();
		}

		int id = pojo.getId();
		if (id <= 0) {
			id = sequence.incrementAndGet();
			pojo.setId(id);
		} else if (store.containsKey(id)) {
			return Response.status(409).entity("Pojo already exists: " + id).build();
		}

		store.put(id, pojo);
		System.out.println("Creating new Pojo: " + pojo);

		return Response.status(201).build();
	}

	@PUT
	@Path("/pojo/update")
	@Consumes({ MediaType.APPLICATION_JSON })
	public Response update(Pojo pojo) {

		if (pojo == null || pojo.getId() <= 0) {
			return Response.status(400).entity("Valid id required").build();
		}

		int id = pojo.getId();
		if (!store.containsKey(id)) {
			return Response.status(404).entity("Pojo not found: " + id).build();
		}

		store.put(id, pojo);
		System.out.println("Updating the Pojo: " + pojo);

		return Response.status(204).build();
	}

	@DELETE
	@Path("/pojo/remove")
	public Response delete(@QueryParam("id") Integer id) {

		if (id == null || id <= 0) {
			return Response.status(400).entity("Valid id required").build();
		}

		Pojo removed = store.remove(id);
		if (removed == null) {
			return Response.status(404).entity("Pojo not found: " + id).build();
		}

		System.out.println("Removing pojo with id: " + id);

		return Response.status(204).build();
	}

}
