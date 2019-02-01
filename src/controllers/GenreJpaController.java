/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controllers;

import controllers.exceptions.NonexistentEntityException;
import controllers.exceptions.PreexistingEntityException;
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
import models.Genre;

/**
 *
 * @author elena
 */
public class GenreJpaController implements Serializable {

    public GenreJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Genre genre) throws PreexistingEntityException, Exception {
        if (genre.getMovieCollection() == null) {
            genre.setMovieCollection(new ArrayList<Movie>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Collection<Movie> attachedMovieCollection = new ArrayList<Movie>();
            for (Movie movieCollectionMovieToAttach : genre.getMovieCollection()) {
                movieCollectionMovieToAttach = em.getReference(movieCollectionMovieToAttach.getClass(), movieCollectionMovieToAttach.getId());
                attachedMovieCollection.add(movieCollectionMovieToAttach);
            }
            genre.setMovieCollection(attachedMovieCollection);
            em.persist(genre);
            for (Movie movieCollectionMovie : genre.getMovieCollection()) {
                Genre oldGenreIdOfMovieCollectionMovie = movieCollectionMovie.getGenreId();
                movieCollectionMovie.setGenreId(genre);
                movieCollectionMovie = em.merge(movieCollectionMovie);
                if (oldGenreIdOfMovieCollectionMovie != null) {
                    oldGenreIdOfMovieCollectionMovie.getMovieCollection().remove(movieCollectionMovie);
                    oldGenreIdOfMovieCollectionMovie = em.merge(oldGenreIdOfMovieCollectionMovie);
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            if (findGenre(genre.getId()) != null) {
                throw new PreexistingEntityException("Genre " + genre + " already exists.", ex);
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Genre genre) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Genre persistentGenre = em.find(Genre.class, genre.getId());
            Collection<Movie> movieCollectionOld = persistentGenre.getMovieCollection();
            Collection<Movie> movieCollectionNew = genre.getMovieCollection();
            Collection<Movie> attachedMovieCollectionNew = new ArrayList<Movie>();
            for (Movie movieCollectionNewMovieToAttach : movieCollectionNew) {
                movieCollectionNewMovieToAttach = em.getReference(movieCollectionNewMovieToAttach.getClass(), movieCollectionNewMovieToAttach.getId());
                attachedMovieCollectionNew.add(movieCollectionNewMovieToAttach);
            }
            movieCollectionNew = attachedMovieCollectionNew;
            genre.setMovieCollection(movieCollectionNew);
            genre = em.merge(genre);
            for (Movie movieCollectionOldMovie : movieCollectionOld) {
                if (!movieCollectionNew.contains(movieCollectionOldMovie)) {
                    movieCollectionOldMovie.setGenreId(null);
                    movieCollectionOldMovie = em.merge(movieCollectionOldMovie);
                }
            }
            for (Movie movieCollectionNewMovie : movieCollectionNew) {
                if (!movieCollectionOld.contains(movieCollectionNewMovie)) {
                    Genre oldGenreIdOfMovieCollectionNewMovie = movieCollectionNewMovie.getGenreId();
                    movieCollectionNewMovie.setGenreId(genre);
                    movieCollectionNewMovie = em.merge(movieCollectionNewMovie);
                    if (oldGenreIdOfMovieCollectionNewMovie != null && !oldGenreIdOfMovieCollectionNewMovie.equals(genre)) {
                        oldGenreIdOfMovieCollectionNewMovie.getMovieCollection().remove(movieCollectionNewMovie);
                        oldGenreIdOfMovieCollectionNewMovie = em.merge(oldGenreIdOfMovieCollectionNewMovie);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = genre.getId();
                if (findGenre(id) == null) {
                    throw new NonexistentEntityException("The genre with id " + id + " no longer exists.");
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
            Genre genre;
            try {
                genre = em.getReference(Genre.class, id);
                genre.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The genre with id " + id + " no longer exists.", enfe);
            }
            Collection<Movie> movieCollection = genre.getMovieCollection();
            for (Movie movieCollectionMovie : movieCollection) {
                movieCollectionMovie.setGenreId(null);
                movieCollectionMovie = em.merge(movieCollectionMovie);
            }
            em.remove(genre);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Genre> findGenreEntities() {
        return findGenreEntities(true, -1, -1);
    }

    public List<Genre> findGenreEntities(int maxResults, int firstResult) {
        return findGenreEntities(false, maxResults, firstResult);
    }

    private List<Genre> findGenreEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Genre.class));
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

    public Genre findGenre(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Genre.class, id);
        } finally {
            em.close();
        }
    }

    public int getGenreCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Genre> rt = cq.from(Genre.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
    public void clearTbl(String namedQuery)
    {
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
