package org.olat.modules.assessment.manager;

import jakarta.persistence.Tuple;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.Logger;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.GroupRoles;
import org.olat.basesecurity.OrganisationRoles;
import org.olat.basesecurity.OrganisationService;
import org.olat.basesecurity.manager.AuthenticationDAO;
import org.olat.basesecurity.manager.GroupDAO;
import org.olat.basesecurity.manager.OrganisationDAO;
import org.olat.basesecurity.model.OrganisationNode;
import org.olat.core.id.Identity;
import org.olat.core.id.Organisation;
import org.olat.core.logging.Tracing;
import org.olat.ims.qti21.repository.handlers.QTI21AssessmentTestHandler;
import org.olat.modules.assessment.AssessmentEntry;
import org.olat.modules.assessment.MigrationService;
import org.olat.modules.assessment.model.*;
import org.olat.modules.curriculum.manager.CurriculumElementDAO;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.manager.RepositoryEntryDAO;
import org.olat.repository.manager.RepositoryEntryRelationDAO;
import org.olat.user.UserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class MigrationServiceImpl implements MigrationService {

    private static final Logger log = Tracing.createLoggerFor(MigrationServiceImpl.class);
    private static final String regex = "(?<=>)[^<>]+(?=<)|<img[^<>]*>";
    private static final PasswordGenerator passGen = new PasswordGenerator.PasswordGeneratorBuilder()
            .useDigits(true)
            .useLower(true)
            .build();

    @Autowired
    private QTI21AssessmentTestHandler ath;

    @Autowired
    private AuthenticationDAO aDao;

    @Autowired
    private UserManager userManager;

    @Autowired
    private BaseSecurity securityManager;

    @Autowired
    private RepositoryEntryRelationDAO repositoryEntryRelationDao;

    @Autowired
    private OrganisationDAO odao;

    @Autowired
    private RepositoryEntryDAO rDao;

    @Autowired
    private OrganisationService oSer;

    @Autowired
    private AssessmentEntryDAO aeDao;

    @Autowired
    private CurriculumElementDAO creDao;

    @Autowired
    private GroupDAO gDao;

    private static final Map<Long, String> orgMap;

    private static final Map<Long, List<OrganisationRoles>> roleMap;

    static {
        Map<Long, String> aMap = new HashMap<>();
        aMap.put(16l, "Suzuki Auto");
        aMap.put(6l, "Suzuki Moto");
        aMap.put(0l, "Suzuki Marine");
        orgMap = aMap;

        Map<Long, List<OrganisationRoles>> bMap = new HashMap<>();
        bMap.put(86l, List.of(OrganisationRoles.administrator));
        bMap.put(37l, List.of(OrganisationRoles.lecturemanager));
        bMap.put(46l, List.of(OrganisationRoles.author));
        bMap.put(82l, List.of(OrganisationRoles.lecturemanager));
        bMap.put(89l, List.of(OrganisationRoles.lecturemanager));
        bMap.put(116l, List.of(OrganisationRoles.lecturemanager));
        bMap.put(118l, List.of(OrganisationRoles.lecturemanager));
        bMap.put(89l, List.of(OrganisationRoles.user));
        bMap.put(1l, List.of(OrganisationRoles.administrator));
        bMap.put(88l, List.of(OrganisationRoles.lecturemanager));
        bMap.put(65l, List.of(OrganisationRoles.lecturemanager));
        bMap.put(87l, List.of(OrganisationRoles.lecturemanager));
        bMap.put(44l, List.of(OrganisationRoles.administrator));
        bMap.put(96l, List.of(OrganisationRoles.lecturemanager));
        bMap.put(-1l, List.of(OrganisationRoles.sysadmin));
        bMap.put(84l, List.of(OrganisationRoles.lecturemanager));
        bMap.put(94l, List.of(OrganisationRoles.lecturemanager));
        bMap.put(36l, List.of(OrganisationRoles.user));
        bMap.put(47l, List.of(OrganisationRoles.user));
        bMap.put(33l, List.of(OrganisationRoles.administrator));
        bMap.put(117l, List.of(OrganisationRoles.learnresourcemanager, OrganisationRoles.principal));

        bMap.put(45l, List.of(OrganisationRoles.principal, OrganisationRoles.lecturemanager));
        //...
        roleMap = bMap;
    }

    @Override
    public void callMe(String sourceDB, String user, String pass, String org, Identity idn) {
        Connection conn;
        try {
            conn = getSourceConnection(sourceDB, user, pass);
        } catch (SQLException e) {
            log.error("Source connection failed!", e);
            return;
        }
        List<Integer> l;
        try {
            l = getAllTestIds(conn);
        } catch (SQLException e) {
            log.error("Tests fetch failed!", e);
            return;
        }
        for(Integer id : l) {
            migrateTest(conn, org, id, idn);
        }
        log.info("fuck yeah!");
    }

    @Override
    public void callMe(String sourceDB, String user, String pass, String org, Integer tid, Identity idn) {
        Connection conn;
        try {
            conn = getSourceConnection(sourceDB, user, pass);
        } catch (SQLException e) {
            log.error("Source connection failed!", e);
            return;
        }
        migrateTest(conn, org, tid, idn);
    }

    @Override
    public void migrateMe(String sourceDB, String user, String pass, Long uid, Identity idn) {
        Connection conn;
        try {
            conn = getSourceConnection(sourceDB, user, pass);

            migrateUser(conn, getUser(conn, uid), aDao.getTestRisorsMap(), aDao.getCourseRisorsMap(), idn);
        } catch (SQLException e) {
            log.error("Source connection failed!", e);
            return;
        }
    }

    private void migrateTest(Connection conn, String org, Integer tid, Identity idn) {
        List<SectionDTO> ls = null;
        String[] to = null;
        Map<String, Integer> meta = null;
        try {
            to = getTestTitle(conn, tid);
            List<RawQuestionContentDTO> listOfRaw = getTestRawQuestions(conn, tid);
            if(!listOfRaw.isEmpty()) {
                ls = buildFromRaw(listOfRaw);
            }
            meta = getMeta(conn, tid);
        } catch (SQLException e) {
            log.error("Test fetch failed! ({0})", tid, e);
            return;
        }

        ath.migrateTest(to, ls, idn, org, tid, meta);

        log.info("Test migration successful!");
    }

    private static Connection getSourceConnection(String sourceDB, String user, String pass) throws SQLException {
        String url = "jdbc:postgresql://" + sourceDB;
        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", pass);
        //props.setProperty("ssl", "true");
        return DriverManager.getConnection(url, props);
    }

    private static Map<String, Integer> getMeta(Connection conn, Integer testId) throws SQLException {
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            st = conn.prepareStatement("select ilosc_pytan, czas_egzaminu, prog_zaliczenia, liczba_prob, difficulty_easy, difficulty_middle, difficulty_hard from cours_tests where cours_tests_id = ?");
            st.setInt(1, testId);
            rs = st.executeQuery();
            rs.next();
            Map<String, Integer> ret = new HashMap<>();
            ret.put("ilosc_pytan", Integer.valueOf(rs.getString("ilosc_pytan")));
            ret.put("czas_egzaminu", Integer.valueOf(rs.getString("czas_egzaminu")));
            ret.put("prog_zaliczenia", Integer.valueOf(rs.getString("prog_zaliczenia")));
            ret.put("liczba_prob", Integer.valueOf(rs.getString("liczba_prob")));
            ret.put("difficulty_easy", Integer.valueOf(rs.getString("difficulty_easy")));
            ret.put("difficulty_middle", Integer.valueOf(rs.getString("difficulty_middle")));
            ret.put("difficulty_hard", Integer.valueOf(rs.getString("difficulty_hard")));
            return ret;
        } finally {
            st.close();
            rs.close();
        }
    }

    private static List<RawQuestionContentDTO> getTestRawQuestions(Connection conn, Integer testId) throws SQLException {
        PreparedStatement st = null;
        ResultSet rs = null;
        List<RawQuestionContentDTO> ret = new ArrayList<>();
        try {
            st = conn.prepareStatement("SELECT title, tresc, questions, corect, class_name, difficulty FROM cours_questions WHERE " +
                    "test_id = ?");
            st.setInt(1, testId);
            rs = st.executeQuery();
            while (rs.next()) {
                ret.add(new RawQuestionContentDTO(rs.getString("title"), rs.getString("tresc"),
                        rs.getString("questions"), rs.getString("corect"), rs.getString("class_name"), rs.getInt("difficulty")));
            }
        } finally {
            st.close();
            rs.close();
        }
        return ret;
    }

    /*private static List<UserDTO> getUsers(Connection connection) throws SQLException {
        PreparedStatement st = null;
        ResultSet rs = null;
        List<UserDTO> ret = new ArrayList<>();
        try {
            st = connection.prepareStatement("select user_id, login, name, surname, organization_id from lw_user where active = 1");
            rs = st.executeQuery();
            while(rs.next()) {
                ret.add(new UserDTO(rs.getString("name"), rs.getString("surname"), rs.getString("email"),
                        rs.getLong("user_id"), rs.getLong("organization_id"), rs.getString("login")));
            }
        } finally {
            st.close();
            rs.close();
        }
        return ret;
    }*/

    private static UserDTO getUser(Connection connection, Long uid) throws SQLException {
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            st = connection.prepareStatement("select user_id, login, name, surname, organization_id, organization_structure_id, email from lw_user where user_id = ?");
            st.setInt(1, uid.intValue());
            rs = st.executeQuery();
            if(!rs.next()) throw new SQLException();
            return new UserDTO(rs.getString("name"), rs.getString("surname"), rs.getString("email"),
                    rs.getLong("user_id"), rs.getLong("organization_id"), rs.getInt("organization_structure_id"), rs.getString("login"));

        } finally {
            st.close();
            rs.close();
        }
    }

    private static String[] getTestTitle(Connection conn, Integer testId) throws SQLException {
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            st = conn.prepareStatement("select title, opis from cours_tests where cours_tests_id = ?");
            st.setInt(1, testId);
            rs = st.executeQuery();
            rs.next();
            return new String[]{rs.getString("title"), rs.getString("opis")};
        } finally {
            st.close();
            rs.close();
        }
    }

    private static List<Integer> getAllTestIds(Connection conn) throws SQLException {
        List<Integer> ret = new ArrayList<>();
        Statement st = null;
        ResultSet rs = null;
        try {
            st = conn.createStatement();
            rs = st.executeQuery("SELECT distinct test_id FROM cours_questions");
            while (rs.next()) {
                ret.add(rs.getInt("test_id"));
            }
            return ret;
        } finally {
            st.close();
            rs.close();
        }
    }

    private static List<SectionDTO> buildFromRaw(List<RawQuestionContentDTO> dtos) {
        List<SectionDTO> ans = new ArrayList<>();
        Map<String, List<RawQuestionContentDTO>> secs = dtos.stream().collect(Collectors.groupingBy(RawQuestionContentDTO::getGroup));
        for(String t : secs.keySet()) {
            List<QuestionDTO> l = secs.get(t).stream().map(rqdto -> rqdto.getType().equals("qnSelect") || rqdto.getType().equals("qnSelectOne") || rqdto.getType().equals("qnSelectBoolean") ?
                            new QuestionDTO(rqdto.getType(), parseQuestion(rqdto.getQuestion()),
                                    parseAnswers(rqdto.getAnswers()), parseCorrect(rqdto.getCorrect()), rqdto.getDifficulty()) :
                            new QuestionDTO(rqdto.getType(), parseQuestion(rqdto.getQuestion()), new ArrayList<>(), new ArrayList<>(), rqdto.getDifficulty()))
                    .collect(Collectors.toList());
            ans.add(new SectionDTO(t, l));
        }
        return ans;
    }

    private static String parseQuestion(String sQst) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher("<p>" + sQst + "</p>");
        String qst = "";
        while(matcher.find()) {
            String s = StringEscapeUtils.unescapeHtml4(matcher.group());
            if(s.contains("<img")) {
                s.replace('<', '[');
                s.replace('>', ']');
                log.info("image to fill {0}", s);
            }
            qst += s;
        }
        return qst;
    }

    private static List<Integer> parseCorrect(String cor) {
        String crt = cor.substring(cor.lastIndexOf("{") + 1, cor.indexOf("}"));
        List<Integer> lcor = new ArrayList<>();
        String[] cp = crt.split(";");
        try {
            for (int i = 0; i < cp.length; i += 2) {
                lcor.add(Integer.parseInt(cp[i].split(":")[1]) - 1);
            }
        } catch (ArrayIndexOutOfBoundsException e) {}
        return lcor;
    }

    private static List<String> parseAnswers(String ans) {
        String ansersy = ans.substring(ans.lastIndexOf("{") + 1, ans.indexOf("}"));
        List<String> lans = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(ansersy, ";");
        while(st.hasMoreElements()) {
            String[] is = st.nextToken().split(":");
            if(is.length < 3) continue;
            lans.add(is[2].substring(1, is[2].length()-1));
        }
        return lans;
    }

    private static DealerDTO getStructure(Connection conn, Integer sid) throws SQLException {
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            st = conn.prepareStatement("select name, organization_structure_id, parent_id from organization_structure where organization_structure_id = ?");
            st.setInt(1, sid);
            rs = st.executeQuery();
            rs.next();
            return new DealerDTO(rs.getString("name"), rs.getInt("organization_structure_id"), rs.getInt("parent_id"));
        } finally {
            st.close();
            rs.close();
        }
    }

    private Organisation resolveUserOrganization(Connection conn, Map<Long, Organisation> top, UserDTO usr, Identity idn) throws  SQLException {
        if(usr.getDealerId() == null || usr.getDealerId() == 0 || usr.getDealerId() == -1)
            return top.get(usr.getOrganisationId());

        DealerDTO dealer = getStructure(conn, usr.getDealerId());
        while(dealer.getParent() != 0 && dealer.getParent() != -1 && dealer.getParent() != null) {
            DealerDTO buf = getStructure(conn, dealer.getParent());
            buf.setChild(dealer);
            dealer = buf;
        }

        OrganisationNode org = odao.getDescendantTree(top.get(usr.getOrganisationId()));
        while(dealer != null) {
            String name = dealer.getName();
            OrganisationNode res = null;
            if(org.getChildrenNode() != null) {
                res = org.getChildrenNode().stream()
                        .filter(o -> o.getOrganisation().getDisplayName().equals(name))
                        .findAny().orElse(null);
            }
            if(res == null) res = odao.getDescendantTree(oSer.createOrganisation(name, name, "", org.getOrganisation(), null, idn));
            org = res;
            dealer = dealer.getChild();
        }

        return org.getOrganisation();
    }

    private void migrateUser(Connection conn, UserDTO userDTO, Map<Long, Tuple> risorsMap, Map<Long, Tuple> courseRisorsMap, Identity me) {
        String pass = passGen.generate(8);

        try {
/*
            Map<Long, Organisation> top = orgMap.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> odao.loadByLabel(e.getValue()).get(0)));

            User user = userManager.createUser(userDTO.getFirstName(), userDTO.getLastName(), userDTO.getEmail());

            Organisation uo = resolveUserOrganization(conn, top, userDTO);

            Identity userIdn = securityManager.createAndPersistIdentityAndUserWithOrganisation(userDTO.getLogin(), userDTO.getLogin(),
                    null, user, BaseSecurityModule.getDefaultAuthProviderIdentifier(), BaseSecurity.DEFAULT_ISSUER,
                    null, userDTO.getLogin(), pass, uo, null);

            userIdn.getUser().setProperty(UserConstants.ORGUNIT, getUsersPositions(conn, userDTO.getLwId()).stream().collect(Collectors.joining(",")));
            userManager.updateUserFromIdentity(userIdn);

            Roles ir = securityManager.getRoles(userIdn);
            RolesByOrganisation editedOrganisationRoles = ir.getRoles(uo);
            if(editedOrganisationRoles == null) {
                editedOrganisationRoles = new RolesByOrganisation(uo, OrganisationRoles.EMPTY_ROLES);
            }
            Long lwr = getUserRole(conn, userDTO.getLwId());
            RolesByOrganisation updatedRoles = RolesByOrganisation.enhance(editedOrganisationRoles, roleMap.get(lwr), Collections.emptyList());
            securityManager.updateRoles(me, userIdn, updatedRoles);

            aDao.insertMigrated(userDTO.getLogin(), pass);
*/

            Identity userIdn = securityManager.findIdentityByLogin(userDTO.getLogin());

            if (userIdn == null) {
                log.error("User identity not found: " + userDTO.getLogin());
                return;
            }
            int deletedEntries = aeDao.deleteEntryForIdentity(userIdn);

            log.info("Deleted " + deletedEntries + " entries as preparation");

            List<Long> tst = getAttendedTests(conn, userDTO.getLwId());
            for (Long tstId : tst) {
                if(risorsMap.containsKey(tstId)) {
                    Tuple p = risorsMap.get(tstId);
                    RepositoryEntry testEntry = rDao.loadByKey(p.get("ot_id", Long.class));
                    RepositoryEntry courseEntry = rDao.loadByKey(p.get("oc_id", Long.class));
                    Long resourceId = p.get("risors_id", Long.class);
                    String sIdent = resourceId != null && !resourceId.equals(0L) ? resourceId.toString() :
                            "n";
                    AssessmentEntry ae = aeDao.createAssessmentEntry(userIdn, null, courseEntry, sIdent, true, testEntry);
                    ae.setAssessmentStatus(AssessmentEntryStatus.done);
                    ae.setMaxScore(new BigDecimal(p.get("max_score", Long.class)));

                    Integer att = getMaxAttempts(conn, userDTO.getLwId(), tstId);
                    ae.setAttempts(att);
                    ae.setScore(new BigDecimal(getPoints(conn, userDTO.getLwId(), tstId, att) / p.get("score_multiplier", Integer.class)));
                    ae.setWeightedScore(ae.getScore());
                    boolean passed = ae.getScore().compareTo(new BigDecimal(p.get("cut_val", Long.class))) >= 0;
                    ae.setPassed(passed);
                    ae.setUserVisibility(true);
                    ae.setFullyAssessed(true);
                    ae.setCurrentRunStatus(AssessmentRunStatus.done);

                    aeDao.updateAssessmentEntry(ae);
                    repositoryEntryRelationDao.addRole(userIdn, courseEntry, GroupRoles.participant.name());
                    log.info("Outcome migration sakces " + userDTO.getLwId() + " " + tstId);
                } else {
                    log.warn("Skipping unmapped test " + tstId);
                }
            }

            for(Map.Entry<Integer, List<Pair<Integer, Date>>> e : getCourseHistory(conn, userDTO.getLwId()).entrySet()) {
                if(courseRisorsMap.containsKey(Long.valueOf(e.getKey())) && !hasTestAttached(conn, e.getKey())) {
                    Tuple p = courseRisorsMap.get(Long.valueOf(e.getKey()));
                    RepositoryEntry courseEntry = rDao.loadByKey(p.get("oc_id", Long.class));
                    String sIdent = p.get("risors_id", Long.class).toString();

                    AssessmentEntry ae = aeDao.createAssessmentEntry(userIdn, null, courseEntry, sIdent, true, null);

                    ae.setScore(new BigDecimal(e.getValue().stream().max(Comparator.comparing(l -> l.getRight())).get().getLeft()));
                    ae.setMaxScore(new BigDecimal(100));
                    ae.setPassed(ae.getScore().equals(ae.getMaxScore()));
                    ae.setUserVisibility(true);
                    ae.setFullyAssessed(true);
                    ae.setCurrentRunStatus(AssessmentRunStatus.done);
                    aeDao.updateAssessmentEntry(ae);
                    repositoryEntryRelationDao.addRole(userIdn, courseEntry, GroupRoles.participant.name());
                } else {
                    log.warn("Skipping unmapped course result " + e.getKey());
                }
            }

/*            try {
                for (Pair<String, Integer> name : getAssignedLearnPaths(conn, userDTO.getLwId())) {
                    CurriculumElement ce = creDao.loadMe(name.getLeft().toLowerCase(), orgMap.get(Long.valueOf(name.getRight())).toLowerCase());
                    if(ce == null) {
                        log.warn("Curriculum element not found by name and org " + name.getLeft() + orgMap.get(Long.valueOf(name.getRight())));
                        continue;
                    }
                    gDao.addMembershipOneWay(ce.getGroup(), userIdn, GroupRoles.participant.name());
                }
            } catch (SQLException e){
                log.warn("Curriculum fetch problem for user " + userDTO.getLogin());
            } catch (Exception ee) {
                log.warn("Some other fakap for learning path assignement for user " + userDTO.getLogin());
            }*/
        } catch (SQLException sqlException) {
            log.warn("Result migration fail for user " + userDTO.getLogin(), sqlException);
            return;
        }
    }

    private static Date getPassedDate(Connection conn, Long lwId, Long courseId, Integer att) {

        ResultSet rs = null;

        Date result = new Date();

        try (PreparedStatement ps = conn.prepareStatement("select max(pass_date) from cours_user where cours_id = ? and user_id = ?")) {
            ps.setLong(1, courseId);
            ps.setLong(2, lwId);
            rs = ps.executeQuery();
            while (rs.next()) {
                result = rs.getDate(1);
            }
        } catch (SQLException e) {
            log.warn("Error fetching passed date");
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException ignored) {

            }
        }

        return result;

    }

    private static List<Long> getAttendedTests(Connection conn, Long uid) throws SQLException {
        List<Long> ret = new ArrayList<>();
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            st = conn.prepareStatement("select cours_tests_id from cours_tests where cours_tests_id in (select distinct test_id from cours_tests_answer where user_id = ?)");
            st.setInt(1, uid.intValue());
            rs = st.executeQuery();
            while (rs.next()) {
                ret.add(Long.valueOf(rs.getInt("cours_tests_id")));
            }
            return ret;
        } finally {
            st.close();
            rs.close();
        }
    }

    private static Integer getMaxAttempts(Connection conn, Long uid, Long tid) throws SQLException {
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            st = conn.prepareStatement("select max(podejscie) from cours_tests_answer where user_id = ? and test_id = ? group by user_id, test_id");
            st.setInt(1, uid.intValue());
            st.setInt(2, tid.intValue());
            rs = st.executeQuery();
            rs.next();
            return rs.getInt(1);
        } finally {
            st.close();
            rs.close();
        }
    }

    private static Long getUserRole(Connection conn, Long uid) throws SQLException {
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            st = conn.prepareStatement("select role_id from user_role where user_id = ?");
            st.setInt(1, uid.intValue());
            rs = st.executeQuery();
            rs.next();
            return Long.valueOf(rs.getInt(1));
        } finally {
            st.close();
            rs.close();
        }
    }

    private static Integer getPoints(Connection conn, Long uid, Long tid, Integer att) throws SQLException {
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            st = conn.prepareStatement("select sum(punkty) from cours_tests_answer where user_id = ? and test_id = ? and podejscie = ? group by user_id, test_id");
            st.setInt(1, uid.intValue());
            st.setInt(2, tid.intValue());
            st.setInt(3, att);
            rs = st.executeQuery();
            rs.next();
            return rs.getInt(1);
        } finally {
            st.close();
            rs.close();
        }
    }

    private static Map<Integer, List<Pair<Integer, Date>>> getCourseHistory(Connection conn, Long uid) throws SQLException {
        Map<Integer, List<Pair<Integer, Date>>> ret = new HashMap<>();
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            st = conn.prepareStatement("SELECT cu.cours_id as cours_id, cu.score score, cu.pass_date pass_date FROM cours_user cu inner join lw_user u on cu.user_id = u.user_id where cu.status = 5 and u.deleted = 0 and u.active = 1 and cu.user_id = ?");
            st.setInt(1, uid.intValue());
            rs = st.executeQuery();
            while(rs.next()) {
                Integer cid = rs.getInt("cours_id");
                List<Pair<Integer, Date>> l = ret.get(cid);
                if(l == null) {
                    l = new ArrayList<>();
                    l.add(new ImmutablePair<>(rs.getInt("score"), rs.getDate("pass_date")));
                    ret.put(cid, l);
                } else {
                    l.add(new ImmutablePair<>(rs.getInt("score"), rs.getDate("pass_date")));
                }
            }
        } finally {
            st.close();
            rs.close();
        }
        return ret;
    }

    private static boolean hasTestAttached(Connection conn, Integer cid) throws SQLException {
        PreparedStatement st = null;
        ResultSet rs = null;
        Integer cnt = 0;
        try {
            st = conn.prepareStatement("select count(cours_id) from cours_tests where cours_id = ?");
            st.setInt(1, cid);
            rs = st.executeQuery();
            rs.next();
            cnt = rs.getInt(1);
        } finally {
            st.close();
            rs.close();
        }
        return cnt > 0;
    }

    private List<String> getUsersPositions(Connection conn, Long uid) throws SQLException{
        PreparedStatement st = null;
        ResultSet rs = null;
        List<String> poss = new ArrayList<>();
        try {
            st = conn.prepareStatement("select stanowisko from lw_user_extended where user_id = ?");
            st.setInt(1, uid.intValue());
            rs = st.executeQuery();
            while(rs.next()) {
                poss.add(rs.getString("stanowisko"));
            }
        } finally {
            st.close();
            rs.close();
        }
        return poss;
    }

    private List<Pair<String, Integer>> getAssignedLearnPaths(Connection conn, Long uid) throws SQLException {
        PreparedStatement st = null;
        ResultSet rs = null;
        List<Pair<String, Integer>> curs = new ArrayList<>();
        try {
            st = conn.prepareStatement("select name, organization_id from learning_path lp where exists " +
                    "(select 1 from learning_path_user lpu where lp.learning_path_id = lpu.learning_path_id and user_id = ?)");
            st.setInt(1, uid.intValue());
            rs = st.executeQuery();
            while(rs.next()) {
                curs.add(new ImmutablePair<>(rs.getString("name"), rs.getInt("organization_id")));
            }
        } finally {
            st.close();
            rs.close();
        }
        return curs;
    }
}
