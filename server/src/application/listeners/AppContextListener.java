// server/src/application/listeners/AppContextListener.java
package application.listeners;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import users.UserManager;

@WebListener
public class AppContextListener implements ServletContextListener {
    public static final String ATTR_USERS = "userManager";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        ctx.setAttribute(ATTR_USERS, new UserManager());
    }

    public static UserManager getUsers(ServletContext ctx) {
        return (UserManager) ctx.getAttribute(ATTR_USERS);
    }
}
