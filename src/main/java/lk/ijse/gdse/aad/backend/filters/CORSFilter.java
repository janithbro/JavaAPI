package lk.ijse.gdse.aad.backend.filters;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
@WebFilter
public class CORSFilter extends HttpFilter {
    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        System.out.println("CORS filter");
        String origin = req.getHeader("Origin");

        if(origin != null && origin.contains("http://localhost:63342")){
            System.out.println("CORS OK");
            res.setHeader("Access-Control-Allow-Origin",origin);
            res.setHeader("Access-Control-Allow-Methods","GET,POST,PUT,DELETE,HEADER");
            res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
            res.setHeader("Access-Control-Expose-Headers","Content-Type");
        }
        chain.doFilter(req,res);
    }
}
