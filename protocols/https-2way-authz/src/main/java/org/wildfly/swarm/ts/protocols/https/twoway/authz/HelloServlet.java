package org.wildfly.swarm.ts.protocols.https.twoway.authz;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/")
public class HelloServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.getWriter().print("Hello on port " + req.getLocalPort()
                + ", secure: " + req.isSecure()
                + ", principal: " + req.getUserPrincipal()
                + ", isGuest: " + req.isUserInRole("GuestRole")
                + ", isUser: " + req.isUserInRole("UserRole"));
    }
}
