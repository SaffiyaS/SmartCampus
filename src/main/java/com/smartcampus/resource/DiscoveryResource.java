package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Discovery endpoint mounted on the application root: GET /api/v1
 * Returns API metadata (version, contact) plus hypermedia links to
 * the main collections.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @Context
    private UriInfo uriInfo;

    @GET
    public Map<String, Object> discovery() {
        String base = uriInfo.getBaseUri().toString();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        Map<String, String> links = new LinkedHashMap<>();
        links.put("self", base);
        links.put("rooms", base + "/rooms");
        links.put("sensors", base + "/sensors");

        Map<String, String> contact = new LinkedHashMap<>();
        contact.put("name", "Smart Campus API Team");
        contact.put("email", "smartcampus@example.ac.uk");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Smart Campus API");
        body.put("version", "1.0.0");
        body.put("contact", contact);
        body.put("_links", links);
        return body;
    }
}
