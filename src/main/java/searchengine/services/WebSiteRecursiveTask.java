package searchengine.services;

import searchengine.model.entities.WebSite;

import java.util.HashSet;
import java.util.concurrent.RecursiveTask;

public class WebSiteRecursiveTask extends RecursiveTask<HashSet<WebSite>> {

    @Override
    protected HashSet<WebSite> compute() {
        return null;
    }
}
