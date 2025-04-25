package org.qubership.colly;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.qubership.colly.db.Cluster;
import org.qubership.colly.db.Environment;

import java.util.List;

@Path("/colly")
public class ClusterResourcesRest {
    @Inject
    CollyStorage collyStorage;


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/clusters")
    public List<Cluster> getClusters() {
        return collyStorage.getClusters();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/environments")
    public List<Environment> getEnvironments() {
        return collyStorage.getEnvironments();
    }

    @POST
    @Path("/tick")
    @Produces(MediaType.APPLICATION_JSON)
    public void loadEnvironmentsManually() {
        collyStorage.executeTask();
    }

    @POST
    @Path("/environments/{envId}")
    public void saveEnvironment(@PathParam("envId") String id,
                                @FormParam("name") String name,
                                @FormParam("owner") String owner,
                                @FormParam("description") String description,
                                @FormParam("status") String status,
                                @FormParam("labels") List<String> labels) {
        collyStorage.saveEnvironment(id, name, owner, description, status, labels);
    }


}

