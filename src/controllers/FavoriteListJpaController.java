/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controllers;

import controllers.exceptions.NonexistentEntityException;
import java.io.Serializable;
import javax.persistence.Query;
import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import models.Movie;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import models.FavoriteList;

/**
 *
 * @author elena
 */
public class FavoriteListJpaController implements Serializable {

    public FavoriteListJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(FavoriteList favoriteList) {
        if (favoriteList.getMovieCollection() == null) {
            favoriteList.setMovieCollection(new ArrayList<Movie>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Collection<Movie> attachedMovieCollection = new ArrayList<Movie>();
            for (Movie movieCollectionMovieToAttach : favoriteList.getMovieCollection()) {
                movieCollectionMovieToAttach = em.getReference(movieCollectionMovieToAttach.getClass(), movieCollectionMovieToAttach.getId());
                attachedMovieCollection.add(movieCollectionMovieToAttach);
            }
            favoriteList.setMovieCollection(attachedMovieCollection);
            em.persist(favoriteList);
            for (Movie movieCollectionMovie : favoriteList.getMovieCollection()) {
                FavoriteList oldFavoriteListIdOfMovieCollectionMovie = movieCollectionMovie.getFavoriteListId();
                movieCollectionMovie.setFavoriteListId(favoriteList);
                movieCollectionMovie = em.merge(movieCollectionMovie);
                if (oldFavoriteListIdOfMovieCollectionMovie != null) {
                    oldFavoriteListIdOfMovieCollectionMovie.getMovieCollection().remove(movieCollectionMovie);
                    oldFavoriteListIdOfMovieCollectionMovie = em.merge(oldFavoriteListIdOfMovieCollectionMovie);
                }
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(FavoriteList favoriteList) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            FavoriteList persistentFavoriteList = em.find(FavoriteList.class, favoriteList.getId());
            Collection<Movie> movieCollectionOld = persistentFavoriteList.getMovieCollection();
            Collection<Movie> movieCollectionNew = favoriteList.getMovieCollection();
            Collection<Movie> attachedMovieCollectionNew = new ArrayList<Movie>();
            for (Movie movieCollectionNewMovieToAttach : movieCollectionNew) {
                movieCollectionNewMovieToAttach = em.getReference(movieCollectionNewMovieToAttach.getClass(), movieCollectionNewMovieToAttach.getId());
                attachedMovieCollectionNew.add(movieCollectionNewMovieToAttach);
            }
            movieCollectionNew = attachedMovieCollectionNew;
            favoriteList.setMovieCollection(movieCollectionNew);
            favoriteList = em.merge(favoriteList);
            for (Movie movieCollectionOldMovie : movieCollectionOld) {
                if (!movieCollectionNew.contains(movieCollectionOldMovie)) {
                    movieCollectionOldMovie.setFavoriteListId(null);
                    movieCollectionOldMovie = em.merge(movieCollectionOldMovie);
                }
            }
            for (Movie movieCollectionNewMovie : movieCollectionNew) {
                if (!movieCollectionOld.contains(movieCollectionNewMovie)) {
                    FavoriteList oldFavoriteListIdOfMovieCollectionNewMovie = movieCollectionNewMovie.getFavoriteListId();
                    movieCollectionNewMovie.setFavoriteListId(favoriteList);
                    movieCollectionNewMovie = em.merge(movieCollectionNewMovie);
                    if (oldFavoriteListIdOfMovieCollectionNewMovie != null && !oldFavoriteListIdOfMovieCollectionNewMovie.equals(favoriteList)) {
                        oldFavoriteListIdOfMovieCollectionNewMovie.getMovieCollection().remove(movieCollectionNewMovie);
                        oldFavoriteListIdOfMovieCollectionNewMovie = em.merge(oldFavoriteListIdOfMovieCollectionNewMovie);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = favoriteList.getId();
                if (findFavoriteList(id) == null) {
                    throw new NonexistentEntityException("The favoriteList with id " + id + " no longer exists.");
                }
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void destroy(Integer id) throws NonexistentEntityException {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            FavoriteList favoriteList;
            try {
                favoriteList = em.getReference(FavoriteList.class, id);
                favoriteList.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The favoriteList with id " + id + " no longer exists.", enfe);
            }
            Collection<Movie> movieCollection = favoriteList.getMovieCollection();
            for (Movie movieCollectionMovie : movieCollection) {
                movieCollectionMovie.setFavoriteListId(null);
                movieCollectionMovie = em.merge(movieCollectionMovie);
            }
            em.remove(favoriteList);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<FavoriteList> findFavoriteListEntities() {
        return findFavoriteListEntities(true, -1, -1);
    }

    public List<FavoriteList> findFavoriteListEntities(int maxResults, int firstResult) {
        return findFavoriteListEntities(false, maxResults, firstResult);
    }

    private List<FavoriteList> findFavoriteListEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(FavoriteList.class));
            Query q = em.createQuery(cq);
            if (!all) {
                q.setMaxResults(maxResults);
                q.setFirstResult(firstResult);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public FavoriteList findFavoriteList(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(FavoriteList.class, id);
        } finally {
            em.close();
        }
    }

    public int getFavoriteListCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<FavoriteList> rt = cq.from(FavoriteList.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
    public void clearTbl(String namedQuery){
        EntityManager em = getEntityManager();
        try 
        { 
            em.getTransaction().begin();
            Query query = em.createNamedQuery(namedQuery);
            query.executeUpdate();
            em.getTransaction().commit();
        } 
        catch (Exception e) 
        { 
            em.getTransaction().rollback();
        }  
    }
    
}
