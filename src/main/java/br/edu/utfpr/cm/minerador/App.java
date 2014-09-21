package br.edu.utfpr.cm.minerador;

import br.edu.utfpr.cm.minerador.model.issue.Issue;
import br.edu.utfpr.cm.minerador.model.svn.Scmlog;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import org.hibernate.Session;

/**
 * Hello world!
 *
 *
 * HADOOP-NUMERO, MAPREDUCE-NUMERO e HDFS-NUMERO
 *
 *
 */
public class App {

    private static EntityManager emSvn;
    private static EntityManager emIssues;

    public static void main(String[] args) {
        connect();

        execute();

    }

    private static void connect() {
        emSvn = Persistence.createEntityManagerFactory("SVN").createEntityManager();
        emIssues = Persistence.createEntityManagerFactory("ISSUE").createEntityManager();

        try {
            emIssues.unwrap(Session.class).connection().prepareStatement(
                    "CREATE TABLE IF NOT EXISTS `issues_scmlog` ( "
                    + "  `id` INT NOT NULL AUTO_INCREMENT, "
                    + "  `issue_id` INT NOT NULL, "
                    + "  `scmlog_id` INT NOT NULL, "
                    + "  PRIMARY KEY (`id`), "
                    + "  UNIQUE INDEX `unq_issue_scmlog` (`issue_id` ASC, `scmlog_id` ASC));").execute();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void execute() {
        final Pattern regex = Pattern.compile("A(?i)RIES-\\d+", Pattern.MULTILINE);

        //List<Issue> issues = emIssues.createQuery("SELECT p FROM Issue p").getResultList();
        //System.err.println(issues.size() + " issues");
        List<Scmlog> commitMessages = emSvn.createQuery("SELECT s FROM Scmlog s").getResultList();
        System.err.println(commitMessages.size() + " commit messages");

        int contador = 0;

        emIssues.getTransaction().begin();

        for (Scmlog log : commitMessages) {
            final Matcher matcher = regex.matcher(log.getMessage());
            while (matcher.find()) {
                String group = matcher.group(); // ARIES-1234
                Pattern regexNumber = Pattern.compile("\\d+");
                Matcher matcherNumber = regexNumber.matcher(group);
                if (matcherNumber.find()) {
                    contador++;
                    String number = matcherNumber.group();
                    try {
                        Issue issue = emIssues.createQuery("SELECT p FROM Issue p WHERE p.id = " + number, Issue.class).getSingleResult();
                        issue.addScmlog(log);
                        emIssues.merge(issue);
                        emIssues.flush();
                    } catch (NoResultException e) {
                        System.out.println("Log: " + log.getId() + " / Ocorrência: " + group + " / Issue: " + number + " / Não encontrado na base de dados.");
                    }
                }

            }

        }

        emIssues.getTransaction().commit();

        System.out.println(contador + " ocorrencias");
    }
}