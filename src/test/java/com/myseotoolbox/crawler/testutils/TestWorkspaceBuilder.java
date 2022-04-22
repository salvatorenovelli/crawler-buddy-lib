//package com.myseotoolbox.crawler.testutils;
//
//import com.myseotoolbox.crawler.repository.WorkspaceRepository;
//
//public class TestWorkspaceBuilder {
//    public static final String DEFAULT_WORKSPACE_ORIGIN = "https://testhost";
//    private static final String DEFAULT_OWNER = "user340790";
//    private final WorkspaceRepository workspaceRepository;
//    private final int seqNumber;
//    private String websiteUrl = DEFAULT_WORKSPACE_ORIGIN;
//    private String ownerName = DEFAULT_OWNER;
//
//    public TestWorkspaceBuilder(WorkspaceRepository workspaceRepository, int seqNumber) {
//        this.workspaceRepository = workspaceRepository;
//        this.seqNumber = seqNumber;
//    }
//
//    public TestWorkspaceBuilder withCrawlOrigin(String url) {
//        this.websiteUrl = url;
//        return this;
//    }
//
//    public void save() {
//
//        Workspace entity = new Workspace();
//        entity.setSeqNumber(seqNumber);
//        entity.setWebsiteUrl(websiteUrl);
//        entity.setOwnerName(ownerName);
//        workspaceRepository.save(entity);
//    }
//
//    public TestWorkspaceBuilder withOwner(String ownerName) {
//        this.ownerName = ownerName;
//        return this;
//    }
//}
