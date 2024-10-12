/*
 Etats du client :
DISCONNECTED : le client est déconnecté
SEARCHING : le client recherche le serveur
NOTFOUND : le client n'a pas trouvé le serveur
CONNECTING : le serveur a été trouvé mais le client attend que le serveur valide la demande (utilisateur+mot de passe)
REFUSED : le serveur a refusé la connexion car l'utilisateur ou son mot de passe sont incorrects
CONNECTED : le client est connecté 
DISCONNECTING : le client est entrain de se déconnecter
*/

package com.atoudeft.client;

import java.net.Socket;
import java.io.*;

import com.atoudeft.commun.net.Connexion;
import com.atoudeft.commun.thread.Lecteur;
import com.atoudeft.commun.evenement.Evenement;
import com.atoudeft.commun.evenement.GestionnaireEvenement;
import com.atoudeft.commun.evenement.EvenementUtil;
import com.atoudeft.commun.thread.ThreadEcouteurDeTexte;

/**
 * Cette classe représente un client capable de se connecter à un serveur.
 *
 * @author Abdelmoumène Toudeft (Abdelmoumene.Toudeft@etsmtl.ca)
 * @version 1.0
 * @since 2023-09-01
 */
public class Client implements Lecteur {

    private String adrServeur = Config.ADRESSE_SERVEUR;
    private int portServeur = Config.PORT_SERVEUR;
    private boolean connecte;
    private Connexion connexion;
    private GestionnaireEvenement gestionnaireEvenement;
    private ThreadEcouteurDeTexte ecouteurTexte;

    /**
     * Permet de fournir un autre gestionnaire d'événements en remplacement de celui construit par défaut.
     *
     * @param gestionnaireEvenement GestionnaireEvenement le nouveau gestionnaire d'événements
     */
    public void setGestionnaireEvenementServeur(GestionnaireEvenement gestionnaireEvenement) {
        this.gestionnaireEvenement = gestionnaireEvenement;
    }
    /**
     * Connecte le client au serveur en utilisant un socket. Si la connexion réussit, un objet
     * Connexion est créé qui crée les flux d'entrée/sortie permettant de communiquer du texte
     * avec le serveur.
     *
     * @return boolean true, si la connexion a réussi. false, si la connexion échoue
     * ou si le client était déjà connecté.
     */
    public boolean connecter() {
        boolean resultat = false;
        if (this.isConnecte()) //deja connecte
            return resultat;

        try {
            Socket socket = new Socket(adrServeur, portServeur);
            connexion = new Connexion(socket);

            //On crée l'ecouteur d'evenements par défaut pour le client :
            gestionnaireEvenement = new GestionnaireEvenementClient(this);

            //Démarrer le thread inspecteur de texte:
            ecouteurTexte = new ThreadEcouteurDeTexte(this);
            ecouteurTexte.start();  //la methode run() de l'ecouteur de texte s'execute en parallele avec le reste du programme.
            resultat = true;
            this.setConnecte(true);
        } catch (IOException e) {
            this.deconnecter();
        }
        return resultat;
    }

    /**
     * Déconnecte le client, s'il est connecté, en fermant l'objet Connexion. Le texte "exit" est envoyé au serveur
     * pour l'informer de la déconnexion. Le thread écouteur de texte est arrêté.
     *
     * @return boolean true, si le client s'est déconnecté, false, s'il était déjà déconnecté
     */
    public boolean deconnecter() {
        if (!isConnecte())
            return false;

        connexion.envoyer("exit");
        connexion.close();
        if (ecouteurTexte != null)
            ecouteurTexte.interrupt();
        this.setConnecte(false);
        return true;
    }
    /**
     * Cette méthode vérifie s'il y a du texte qui arrive sur la connexion du client et, si c'est le cas, elle crée
     * un événement contenant les données du texte et demande au gestionnaire d'événement client de traiter l'événement.
     *
     * @author Abdelmoumène Toudeft
     * @version 1.0
     * @since   2023-09-20
     */
    public void lire() {

        String[] t;
        Evenement evenement;
        String texte = connexion.getAvailableText();

        if (!"".equals(texte)){
            t = EvenementUtil.extraireInfosEvenement(texte);
            evenement = new Evenement(connexion,t[0],t[1]);
            gestionnaireEvenement.traiter(evenement);
        }
    }
    /**
     * Cette méthode retourne l'adresse IP du serveur sur lequel ce client se connecte.
     *
     * @return String l'adresse IP du serveur dans le format "192.168.25.32"
     * @author Abdelmoumène Toudeft
     * @version 1.0
     * @since   2023-09-20
     */
    public String getAdrServeur() {
        return adrServeur;
    }
    public void setAdrServeur(String adrServeur) {
        this.adrServeur = adrServeur;
    }
    /**
     * Indique si le client est connecté à un serveur..
     *
     * @return boolean true si le client est connecté et false sinon
     */
    public boolean isConnecte() {
        return connecte;
    }

    /**
     * Marque ce client comme étant connecté ou déconnecté.
     *
     * @param connecte boolean Si true, marque le client comme étant connecté, si false, le marque comme déconnecté
     */
    public void setConnecte(boolean connecte) {
        this.connecte = connecte;
    }

    /**
     * Retourne le port d'écoute du serveur auquel ce client se connecte.
     *
     * @return int Port d'écoute du serveur
     */
    public int getPortServeur() {
        return portServeur;
    }

    /**
     * Spécifie le port d'écoute du serveur sur lequel ce client se connecte.
     *
     * @param portServeur int Port d'écoute du serveur
     */
    public void setPortServeur(int portServeur) {
        this.portServeur = portServeur;
    }

    /**
     * Envoie un texte au serveur en utilisant un objet Connexion.
     *
     * @param s String texte à envoyer
     */
    public void envoyer(String s) {
        this.connexion.envoyer(s);
    }
}