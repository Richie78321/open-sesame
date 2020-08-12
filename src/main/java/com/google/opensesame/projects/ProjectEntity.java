package com.google.opensesame.projects;

import static com.googlecode.objectify.ObjectifyService.ofy;

import com.google.opensesame.github.GitHubGetter;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.OnLoad;
import com.googlecode.objectify.annotation.OnSave;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.kohsuke.github.GHRepository;

/** The class used to interact with the project entities in data storage. */
@Entity
public class ProjectEntity {
  public static final String PROJECT_ID_PARAM = "projectId";
  
  public static final long MAX_GITHUB_SYNC_AGE = 3600000; // 1 hour

  /**
   * Get a project entity with a specified repository ID from the datastore or
   * create a new one if one does not exist in the datastore.
   *
   * @param repositoryId The repository ID of the project.
   * @return Returns the project entity from datastore or a new project entity.
   * @throws IOException
   */
  public static ProjectEntity fromRepositoryIdOrNew(String repositoryId) throws IOException {
    ProjectEntity projectEntity = ofy().load().type(ProjectEntity.class).id(repositoryId).now();
    if (projectEntity == null) {
      projectEntity =
          new ProjectEntity(repositoryId, new ArrayList<String>(), new ArrayList<String>());
    }

    return projectEntity;
  }

  public static void updateGitHubIndexes() throws IOException {
    long maxStaleGitHubSyncAge = System.currentTimeMillis() - MAX_GITHUB_SYNC_AGE;
    System.out.println(maxStaleGitHubSyncAge);
    List<ProjectEntity> projectEntities = ofy()
        .load()
        .type(ProjectEntity.class)
        .filter("timeSyncedWithGitHub <= ", maxStaleGitHubSyncAge)
        .list();

    for (ProjectEntity projectEntity : projectEntities) {
      projectEntity.syncGitHubIfStale();
    }

    ofy().save().entities(projectEntities).now();
  }

  @Id public String repositoryId;
  public List<String> mentorIds;
  public List<String> interestedUserIds;
  @Index protected Integer numMentors;
  @Index protected Integer numInterestedUsers;
  @Index protected Integer numContributors;
  @Index protected Long timeSyncedWithGitHub;

  /**
   * No-arg constructor for Objectify:
   * https://github.com/objectify/objectify/wiki/Entities#the-basics
   * 
   * @throws IOException
   */
  protected ProjectEntity() {}

  /**
   * Creates a new ProjectEntity object.
   *
   * @param repositoryId
   * @param mentorIds
   * @param interestedUserIds
   * @throws IOException
   */
  public ProjectEntity(String repositoryId, List<String> mentorIds, List<String> interestedUserIds)
      throws IOException {
    this.repositoryId = repositoryId;
    this.mentorIds = mentorIds;
    this.interestedUserIds = interestedUserIds;

    syncGitHub();
  }

  /** Compute the lengths of mentor and interested user lists before the project entity is saved. */
  @OnSave
  protected void computeListLengths() {
    numMentors = mentorIds.size();
    numInterestedUsers = interestedUserIds.size();
  }

  @OnLoad
  protected void checkNullLists() {
    if (this.mentorIds == null) {
      this.mentorIds = new ArrayList<String>();
    }
    if (this.interestedUserIds == null) {
      this.interestedUserIds = new ArrayList<String>();
    }
  }

  @OnLoad
  public void syncGitHubIfStale() throws IOException {
    System.out.println(System.currentTimeMillis());
    if (timeSyncedWithGitHub == null ||
        System.currentTimeMillis() - timeSyncedWithGitHub >= MAX_GITHUB_SYNC_AGE) {
      syncGitHub();
    }
  }

  public void syncGitHub() throws IOException {
    // TODO(Richie): When GitHub caching is added, ensure that this GitHub interface is not cached, as
    // multiple layers of caching could lead to data being more out of date than intended.
    if (SYNC_WITH_GITHUB) {
      GHRepository repository = GitHubGetter.getGitHub().getRepositoryById(repositoryId);
      numContributors = repository.listContributors().toArray().length;
      timeSyncedWithGitHub = System.currentTimeMillis();
      System.out.println(numContributors);
    }
  }
}
