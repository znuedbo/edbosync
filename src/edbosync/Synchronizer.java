package edbosync;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;
import com.google.gson.Gson;
import java.sql.Date;
import java.util.Calendar;
import ua.edboservice.ArrayOfDPersonAddRet;
import ua.edboservice.ArrayOfDPersonRequestSeasons;
import ua.edboservice.ArrayOfDPersonsFind;
import ua.edboservice.DPersonRequestSeasons;
import ua.edboservice.DPersonsFind;
import ua.edboservice.EDBOGuides;
import ua.edboservice.EDBOGuidesSoap;
import ua.edboservice.EDBOPerson;
import ua.edboservice.EDBOPersonSoap;
import ua.edboservice.ArrayOfDPersonAddresses;
import ua.edboservice.ArrayOfDPersonBenefits;
import ua.edboservice.ArrayOfDPersonContacts;
import ua.edboservice.ArrayOfDPersonDocuments;
import ua.edboservice.ArrayOfDPersonDocumentsSubjects;
import ua.edboservice.ArrayOfDPersonOlympiadsAwards;
import ua.edboservice.ArrayOfDRequestExaminationCauses;
import ua.edboservice.ArrayOfDUniversityFacultetSpecialities;
import ua.edboservice.ArrayOfDUniversityFacultets;
import ua.edboservice.DPersonAddRet;
import ua.edboservice.DPersonAddresses;
import ua.edboservice.DPersonBenefits;
import ua.edboservice.DPersonContacts;
import ua.edboservice.DPersonDocuments;
import ua.edboservice.DPersonDocumentsSubjects;
import ua.edboservice.DPersonOlympiadsAwards;
import ua.edboservice.DRequestExaminationCauses;
import ua.edboservice.DUniversityFacultetSpecialities;
import ua.edboservice.DUniversityFacultets;
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * Синхронизатор с EDBO
 *
 * @author С.В. Чопоров (S.V. Choporov)
 * @version 1.0
 * @see java.util.AbstractList
 * @see java.util.List
 * @see com.google.gson.Gson
 * @see edbosync.Person
 * @see edbosync.PersonContact
 */
public class Synchronizer {

    private EDBOGuides guidesEdbo = new EDBOGuides();
    /**
     * Соап-поток справочников ЕДБО
     */
    private EDBOGuidesSoap guidesSoap = null;
    // Person soap
    private EDBOPerson personEdbo = new EDBOPerson();
    /**
     * Соап-поток информации о персоне базы ЕДБО
     */
    private EDBOPersonSoap personSoap = null;
    /**
     * Логин для подключения к ЕДБО
     */
    private String soapUser = "pljuta.natalija@edbo.gov.ua";
    /**
     * Пароль для подключения к ЕДБО
     */
    private String soapPassword = "qazse75259";
    /**
     * Ключ клиента-приложения при подключении к ЕДБО
     */
    private String applicationKey = "Y0NzMXVGYnplb2lYZzhxVlA3ZUZ4eFJualhlNnowbkh2dmpTQ0FSNkc5U09iOW9yWExQUnVLZ1FWZVNIQmY5b2JMQ1ZaSHRvcmg5eFFka2pKWGlabUZvVnBFN3hTakZCYUROQkhEQ3FzQUFtTFQ5UzRKOE82a2NGeFJGdUs1rMC=";
    /**
     * Идентификатор текущей соап-сессии
     */
    private String sessionGuid = "";
    /**
     * Текущая дата
     */
    private String actualDate = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(java.util.Calendar.getInstance().getTime());
    /**
     * Идентификатор языка (1 - укр)
     */
    private int languageId = 1;
    /*
     * Идентификатор вступительной компании (2 -- 2012, 3 -- 2013)
     */
    private int seasonId = 2;
    /**
     * Ключ университета в Soap
     */
    private String universityKey = "ab1bc732-51f3-475c-bcfe-368363369020";
    // MySQL
    /**
     * Строка соединения с базой MySQL
     */
    private String mySqlConnectionUrl = "jdbc:mysql://10.1.103.26/abiturient?useUnicode=true&characterEncoding=utf-8";
    /**
     * Пользователь MySQL
     */
    private String mySqlUser = "root";
    /**
     * Пароль MySQL
     */
    private String mySqlPassword = "root";
    /**
     * Обработчик соединения MySQL
     */
    private Connection mySqlConnection = null;
    /**
     * Обработчик запросов MySQL
     */
    private Statement mySqlStatement = null;

    /**
     * Конструктор
     */
    public Synchronizer() {
        System.out.println("Создан объект-синхронизатор EDBO: " + actualDate);
    }

    /**
     * Установка соединения со службой EDBO Guides
     *
     * @return true, если соединение установлено, false - иначе
     */
    protected boolean guidesConnect() {
        guidesSoap = guidesEdbo.getEDBOGuidesSoap();
        sessionGuid = guidesSoap.login(soapUser, soapPassword, 0, applicationKey);
        if (sessionGuid.length() != 36) {
            // при соединении возникла ошибка
            System.out.println(sessionGuid);
            return false;
        }
        return true;
    }

    /**
     * Установка соединения со службой EDBO Person
     *
     * @return true, если соединение установлено, false - иначе
     */
    protected boolean personConnect() {
        // wsdl connection url:
        // http://10.1.103.99:8080/EDBOPerson/EDBOPerson.asmx?WSDL
        personSoap = personEdbo.getEDBOPersonSoap();
//        sessionGuid = personSoap.login(soapUser, soapPassword, 0, applicationKey);
        sessionGuid = personSoap.login("davidovskij.v@edbo.gov.ua", "testpass1917", 0, ""); //// TEST !!!!!!!!!!!!!!!!
        if (sessionGuid.length() != 36) {
            // при соединении возникла ошибка
            System.out.println(sessionGuid);
            return false;
        }
        return true;
    }

    /**
     * Соединение с тестовой базой данных
     *
     * @return true, если соединение установлено, false - иначе
     */
//    protected boolean personConnectTest() {
//        // wsdl connection url:
//        // http://test.edbo.gov.ua:8080/EDBOPerson/EDBOPerson.asmx?WSDL
//        personSoap = personEdbo.getEDBOPersonSoap();
//        sessionGuid = personSoap.login("davidovskij.v@edbo.gov.ua", "testpass1917", 0, "");
//        if (sessionGuid.length() != 36) {
//            // при соединении возникла ошибка
//            System.out.println(sessionGuid);
//            return false;
//        }
//        return true;
//    }
    /**
     * Установить соединение с базой данных MySQL
     *
     * @return true, если соединение установлено, false - иначе
     */
    protected boolean mySqlConnect() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            try {
                try {
                    mySqlConnection = DriverManager.getConnection(mySqlConnectionUrl, mySqlUser, mySqlPassword);
                } catch (SQLException ex) {
                    Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
                }
                mySqlStatement = mySqlConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                        ResultSet.CONCUR_UPDATABLE);
            } catch (SQLException ex) {
                Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    /**
     * Закрыть соединение с базой данных MySQL
     */
    protected void mySqlConnectionClose() {
        try {
            mySqlConnection.close();
        } catch (SQLException ex) {
            Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Список вступительных компаний
     */
    public void personRequestSeasons() {
        if (personConnect()) {
            ArrayOfDPersonRequestSeasons seasonsArray = personSoap.personRequestSeasonsGet(sessionGuid, languageId, "", 0, 0, 1);
            List<DPersonRequestSeasons> seasonsList = seasonsArray.getDPersonRequestSeasons();
            for (DPersonRequestSeasons season : seasonsList) {
                System.out.println(season.getName() + "\t" + season.getRequestPerPerson()
                        + "\t" + season.getClosed() + "\tid: " + season.getIdPersonRequestSeasons());
            }
        }
    }

    /**
     * Загрузка списка специальносте из базы ЕДБО
     */
    public void specialitiesGet() {
        if (guidesConnect() && mySqlConnect()) {
            ArrayOfDUniversityFacultets facultetsArray = guidesSoap.universityFacultetsGet(sessionGuid, universityKey, "", languageId, actualDate, 1, "20", 1, -1, 0, -1);
            List<DUniversityFacultets> facultetsList = facultetsArray.getDUniversityFacultets();
            for (DUniversityFacultets facultet : facultetsList) {
                ArrayOfDUniversityFacultetSpecialities specialitiesArray =
                        guidesSoap.universityFacultetSpecialitiesGet(sessionGuid, universityKey, facultet.getUniversityFacultetKode(), "", languageId, actualDate, seasonId, 0, "", "", "", "");
                List<DUniversityFacultetSpecialities> specialitiesList = specialitiesArray.getDUniversityFacultetSpecialities();
                for (DUniversityFacultetSpecialities speciality : specialitiesList) {
                    String mon_kod = "";
                    if (speciality.getSpecClasifierCode() != null && !speciality.getSpecClasifierCode().isEmpty()) {
                        mon_kod = speciality.getSpecClasifierCode();
                    } else {
                        mon_kod = speciality.getSpecSpecialityClasifierCode();
                    }
                    String sql = "INSERT INTO `abiturient`.`specialities`"
                            + "(`idSpeciality`,"
                            + "`SpecialityName`,"
                            + "`SpecialityDirectionName`,"
                            + "`SpecialitySpecializationName`,"
                            + "`SpecialityKode`,"
                            + "`FacultetID`,"
                            + "`SpecialityClasifierCode`,"
                            + "`SpecialityContractCount`,"
                            + "`PersonEducationFormID`"
                            + ")"
                            + "VALUES"
                            + "("
                            + speciality.getIdUniversitySpecialities() + ","
                            + "'" + speciality.getSpecSpecialityName() + "',"
                            + "'" + speciality.getSpecDirectionName() + "',"
                            + "'" + speciality.getSpecSpecializationName() + "',"
                            + "'" + speciality.getSpecCode() + "',"
                            + facultet.getIdUniversityFacultet() + ","
                            + mon_kod + ","
                            + speciality.getUniversitySpecialitiesContractCount() + ","
                            + speciality.getIdPersonEducationForm()
                            + ");";
                    System.out.println(speciality.getIdUniversitySpecialities() + ","
                            + "'" + speciality.getSpecSpecialityName() + "',"
                            + "'" + speciality.getSpecDirectionName() + "',"
                            + "'" + speciality.getSpecSpecializationName() + "',"
                            + "'" + speciality.getSpecCode() + "',"
                            + facultet.getIdUniversityFacultet() + ","
                            + mon_kod + ","
                            + speciality.getUniversitySpecialitiesContractCount() + ","
                            + speciality.getIdPersonEducationForm());
                    try {
                        ResultSet facultetInMysql = mySqlStatement.executeQuery("SELECT * FROM abiturient.facultets WHERE facultets.idFacultet = "
                                + facultet.getIdUniversityFacultet() + ";");
                        if (!facultetInMysql.next()) {
                            System.out.println("ОШИБКА ФАКУЛЬТЕТА!!!! " + sql);
                        }
                    } catch (SQLException ex) {
                        Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    try {
                        mySqlStatement.executeUpdate(sql);
                    } catch (SQLException ex) {
//                        System.out.println(sql);
//                        System.out.flush();
//                        Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

    /**
     * Поиск персон по серии и номеру документа в базе ЕДБО
     *
     * @param series Серия документа
     * @param number Номер документа
     * @return Строка в формате json с информацией о персоне или "0", если
     * персон не найдено
     * @see edbosync.Person
     */
    public String findPersonEdbo(String series, String number) {
        if (personConnect() && mySqlConnect()) {
            ArrayOfDPersonsFind personArray = personSoap.personsFind(sessionGuid, actualDate, languageId,
                    "*", series, number, "", 1, "", "");
            List<DPersonsFind> personList = personArray.getDPersonsFind();
            if (personList.size() > 0) {
                ArrayList<Person> person;
                person = new ArrayList<Person>();
                for (DPersonsFind dPerson : personList) {
                    Person p = new Person();
                    // базовая информация
                    p.setFirstName(dPerson.getFirstName());
                    p.setMiddleName(dPerson.getMiddleName());
                    p.setLastName(dPerson.getLastName());
                    p.setId_PersonSex(dPerson.getIdPersonSex());
                    p.setBirthday(dPerson.getBirthday().toGregorianCalendar());
                    p.setResident(dPerson.getResident());
                    p.setPersonCodeU(dPerson.getPersonCodeU());
                    p.setId_Person(dPerson.getIdPerson());

                    // адрес персоны
                    ArrayOfDPersonAddresses adressesArray = personSoap.personAddressesGet(sessionGuid, actualDate, languageId, dPerson.getPersonCodeU(), 0);
                    List<DPersonAddresses> adressesList = adressesArray.getDPersonAddresses();
                    if (adressesList.size() > 0) {

                        DPersonAddresses adress = adressesList.get(0); // работаем с первым адресом (нам болше и не нужно)
                        p.setId_StreetType(adress.getIdStreetType());
                        p.setAddress(adress.getAdress());
                        p.setHomeNumber(adress.getHomeNumber());
                        p.setPostIndex(adress.getPostIndex());
                        // определение кодов KOATUU
                        try {
                            ResultSet koatuu_full = mySqlStatement.executeQuery("SELECT idKOATUULevel3, KOATUULevel2ID, "
                                    + "(SELECT `koatuulevel2`.`KOATUULevel1ID` FROM koatuulevel2 WHERE `koatuulevel2`.`idKOATUULevel2` = KOATUULevel2ID) "
                                    + "FROM abiturient.koatuulevel3 WHERE KOATUULevel3FullName like \""
                                    + adress.getKOATUUFullName().replaceAll("'", "\\\\'")
                                    + "\";");
                            if (koatuu_full.next()) {
                                // Запрос вернул информацию о кодах KOATUU на трех уровнях
                                p.setId_KoatuuCodeL3(koatuu_full.getInt(1));
                                p.setId_KoatuuCodeL2(koatuu_full.getInt(2));
                                p.setId_KoatuuCodeL1(koatuu_full.getInt(3));
                            } else {
                                // Если запись на третьем уровне отсутствует (например, у жителей крупных городов),
                                // то поиск осуществляем по второму уровню
                                ResultSet koatuu2 = mySqlStatement.executeQuery("SELECT idKOATUULevel2, KOATUULevel1ID "
                                        + "FROM abiturient.koatuulevel2 WHERE KOATUULevel2FullName like \""
                                        + adress.getKOATUUFullName().replaceAll("'", "\\\\'")
                                        + "\";");
                                if (koatuu2.next()) {
                                    // Запрос вернул информацию о кодах KOATUU на двух уровнях
                                    p.setId_KoatuuCodeL2(koatuu2.getInt(1));
                                    p.setId_KoatuuCodeL1(koatuu2.getInt(2));
                                } else {
                                    // на первом и втором уровнях отсутствуют только нерезиденты
                                    p.setId_KoatuuCodeL2(135607);
                                }
                            }
                        } catch (SQLException ex) {
                            Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

//                    p.setContacts(getPersonContactsEdbo(dPerson.getPersonCodeU()));

                    person.add(p);
                }
                Gson json;
                json = new Gson();
                return json.toJson(person);
            }
        }
        return "0";
    }

    /**
     * Поиск персоны по маске фамилия, имя, отчество
     *
     * @param fioMask маска фио
     * @return Строка в формате json с информацией о персоне или "0", если
     * персон не найдено
     * @see edbosync.Person
     */
    public String findPersonEdbo(String fioMask) {
        if (personConnect() && mySqlConnect()) {
            ArrayOfDPersonsFind personArray = personSoap.personsFind(sessionGuid, actualDate, languageId,
                    fioMask.replaceAll(" ", "*").replaceAll("c|C|i|I", "?"), "", "", "", 1, "", "");
//            System.out.println(fioMask.replaceAll(" ", "*").replaceAll("c|C|i|I", "?"));
            List<DPersonsFind> personList = personArray.getDPersonsFind();
            if (personList.size() > 0) {
                ArrayList<Person> person;
                person = new ArrayList<Person>();
                for (DPersonsFind dPerson : personList) {
                    Person p = new Person();
                    // базовая информация
                    p.setFirstName(dPerson.getFirstName());
                    p.setMiddleName(dPerson.getMiddleName());
                    p.setLastName(dPerson.getLastName());
                    p.setId_PersonSex(dPerson.getIdPersonSex());
                    p.setBirthday(dPerson.getBirthday().toGregorianCalendar());
                    p.setResident(dPerson.getResident());
                    p.setPersonCodeU(dPerson.getPersonCodeU());
                    p.setId_Person(dPerson.getIdPerson());

                    // адрес персоны
                    ArrayOfDPersonAddresses adressesArray = personSoap.personAddressesGet(sessionGuid, actualDate, languageId, dPerson.getPersonCodeU(), 0);
                    List<DPersonAddresses> adressesList = adressesArray.getDPersonAddresses();
                    if (adressesList.size() > 0) {

                        DPersonAddresses adress = adressesList.get(0); // работаем с первым адресом (нам болше и не нужно)
                        p.setId_StreetType(adress.getIdStreetType());
                        p.setAddress(adress.getAdress());
                        p.setHomeNumber(adress.getHomeNumber());
                        p.setPostIndex(adress.getPostIndex());
                        // определение кодов KOATUU
                        try {
                            ResultSet koatuu_full = mySqlStatement.executeQuery("SELECT idKOATUULevel3, KOATUULevel2ID, "
                                    + "(SELECT `koatuulevel2`.`KOATUULevel1ID` FROM koatuulevel2 WHERE `koatuulevel2`.`idKOATUULevel2` = KOATUULevel2ID) "
                                    + "FROM abiturient.koatuulevel3 WHERE KOATUULevel3FullName like \""
                                    + adress.getKOATUUFullName().replaceAll("'", "\\\\'")
                                    + "\";");
                            if (koatuu_full.next()) {
                                // Запрос вернул информацию о кодах KOATUU на трех уровнях
                                p.setId_KoatuuCodeL3(koatuu_full.getInt(1));
                                p.setId_KoatuuCodeL2(koatuu_full.getInt(2));
                                p.setId_KoatuuCodeL1(koatuu_full.getInt(3));
                            } else {
                                // Если запись на третьем уровне отсутствует (например, у жителей крупных городов),
                                // то поиск осуществляем по второму уровню
                                ResultSet koatuu2 = mySqlStatement.executeQuery("SELECT idKOATUULevel2, KOATUULevel1ID "
                                        + "FROM abiturient.koatuulevel2 WHERE KOATUULevel2FullName like \""
                                        + adress.getKOATUUFullName().replaceAll("'", "\\\\'")
                                        + "\";");
                                if (koatuu2.next()) {
                                    // Запрос вернул информацию о кодах KOATUU на двух уровнях
                                    p.setId_KoatuuCodeL2(koatuu2.getInt(1));
                                    p.setId_KoatuuCodeL1(koatuu2.getInt(2));
                                } else {
                                    // на первом и втором уровнях отсутствуют только нерезиденты
                                    p.setId_KoatuuCodeL2(135607);
                                }
                            }
                        } catch (SQLException ex) {
                            Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

//                    p.setContacts(getPersonContactsEdbo(dPerson.getPersonCodeU()));

                    person.add(p);
                }
                Gson json;
                json = new Gson();
                return json.toJson(person);
            }
        }
        return "0";
    }

    /**
     * Получить список контактов персоны в базе ЕДБО
     *
     * @param personCodeU Код персоны в ЕДБО
     * @return Список контактов персоны
     * @see edbosync.PersonContact
     */
    public ArrayList<PersonContact> getPersonContactsEdbo(String personCodeU) {
        ArrayList<PersonContact> contacts;
        contacts = new ArrayList<PersonContact>();
        if (personConnect()) {
            ArrayOfDPersonContacts contactsArray = personSoap.personContactsGet(sessionGuid, actualDate, languageId, personCodeU, 0);
            List<DPersonContacts> contactsList = contactsArray.getDPersonContacts();
            for (DPersonContacts dContact : contactsList) {
                PersonContact contact = new PersonContact();
                contact.setId_Contact(dContact.getIdPersonContact());
                contact.setId_ContactType(dContact.getIdPersonContactType());
                contact.setIsDefault(dContact.getDefaull());
                contact.setValue(dContact.getValue());

                contacts.add(contact);
            }
        }
        return contacts;
    }

    /**
     * Получить список контактов персоны из ЕДБО в формате json
     *
     * @param personCodeU код персоны в базе ЕДБО
     * @return список контактов персоны из ЕДБО в формате json
     */
    public String getPersonContactsEdboJson(String personCodeU) {
        Gson json = new Gson();
        ArrayList<PersonContact> contacts = getPersonContactsEdbo(personCodeU);
        return json.toJson(contacts);
    }

    /**
     * Получить список документов персоны из базы ЕДБО
     *
     * @param personCodeU код персоны в базе ЕДБО
     * @return список документов персоны из базы ЕДБО
     * @see edbosync.PersonDocument
     */
    public ArrayList<PersonDocument> getPersonDocumentEdbo(String personCodeU) {
        ArrayList<PersonDocument> documents = new ArrayList<PersonDocument>();
        if (personConnect()) {
            ArrayOfDPersonDocuments documentsArray = personSoap.personDocumentsGet(sessionGuid, actualDate, languageId, personCodeU, 0, 0, "", -1);
            List<DPersonDocuments> documentsList = documentsArray.getDPersonDocuments();
            for (DPersonDocuments dDocument : documentsList) {
                PersonDocument document = new PersonDocument();
                document.setAttestatValue(dDocument.getAtestatValue());
                document.setDateGet(dDocument.getDocumentDateGet().toGregorianCalendar());
                document.setId_Document(dDocument.getIdPersonDocument());
                document.setId_Type(dDocument.getIdPersonDocumentType());
                document.setIssued(dDocument.getDocumentIssued());
                document.setNumber(dDocument.getDocumentNumbers());
                document.setSeries(dDocument.getDocumentSeries());
                document.setZnoPin(dDocument.getZNOPin());

                if (document.getId_Type() == 4) {
                    // документ является сертификатом ЗНО
                    ArrayList<DocumentSubject> subjects = new ArrayList<DocumentSubject>();
                    ArrayOfDPersonDocumentsSubjects subjectsArray = personSoap.personDocumentsSubjectsGet(sessionGuid, actualDate, languageId, document.getId_Document(), dDocument.getIdPerson(), document.getId_Type());
                    List<DPersonDocumentsSubjects> subjectsList = subjectsArray.getDPersonDocumentsSubjects();
                    for (DPersonDocumentsSubjects dSubject : subjectsList) {
                        DocumentSubject subject = new DocumentSubject();
                        subject.setId_Subject(dSubject.getIdSubject());
                        subject.setSubjectValue(dSubject.getPersonDocumentSubjectValue());
                        subjects.add(subject);
                    }
                    document.setSubjects(subjects);
                }

                documents.add(document);
            }
        }
        return documents;
    }

    /**
     * Получить список документов персоны из базы ЕДБО в формате json
     *
     * @param personCodeU код персоны в базе ЕДБО
     * @return список документов персоны из базы ЕДБО в формате json
     * @see edbosync.PersonDocument
     */
    public String getPersonDocumentEdboJson(String personCodeU) {
        Gson json = new Gson();
        ArrayList<PersonDocument> documents = getPersonDocumentEdbo(personCodeU);
        return json.toJson(documents);
    }

    /**
     * Получить список идентификаторов олимпиад персоны из базы ЕДБО
     *
     * @param personCodeU код персоны в базе ЕДБО
     * @return список идентификаторов олимпиад персоны
     */
    public ArrayList<Integer> getPersonOlympiadsEdbo(String personCodeU) {
        ArrayList<Integer> olympiadId = new ArrayList<Integer>();
        if (personConnect()) {
            ArrayOfDPersonOlympiadsAwards awardsArray = personSoap.personOlympiadsAwardsGet(sessionGuid, actualDate, languageId, personCodeU, seasonId);
            List<DPersonOlympiadsAwards> awardsList = awardsArray.getDPersonOlympiadsAwards();
            for (DPersonOlympiadsAwards award : awardsList) {
                olympiadId.add(award.getIdOlympiadAward());
            }
        }
        return olympiadId;
    }

    /**
     * Получить список идентификаторов олимпиад персоны из базы ЕДБО в формате
     * json
     *
     * @param personCodeU код персоны в базе ЕДБО
     * @return список идентификаторов олимпиад персоны в формате json
     */
    public String getPersonOlympiadsEdboJson(String personCodeU) {
        Gson json = new Gson();
        ArrayList<Integer> olympiadId = getPersonOlympiadsEdbo(personCodeU);
        return json.toJson(olympiadId);
    }

    /**
     * Получить список идентификаторов льгот персоны из базы ЕДБО
     *
     * @param personId идентификатор персоны в базе ЕДБО
     * @return список идентификаторов льгот персоны из базы ЕДБО
     */
    public ArrayList<Integer> getPersonBenefitsEdbo(int personId) {
        ArrayList<Integer> benefitId = new ArrayList<Integer>();
        if (personConnect()) {
            ArrayOfDPersonBenefits benefitsArray = personSoap.personBenefitsGet(sessionGuid, actualDate, languageId, personId);
            List<DPersonBenefits> benefitsList = benefitsArray.getDPersonBenefits();
            for (DPersonBenefits benefit : benefitsList) {
                benefitId.add(benefit.getIdBenefit());
            }
        }
        return benefitId;
    }

    /**
     * Получить список идентификаторов льгот персоны из базы ЕДБО в формате json
     *
     * @param personId идентификатор персоны в базе ЕДБО
     * @return список идентификаторов льгот персоны из базы ЕДБО в формате json
     */
    public String getPersonBenefitsEdboJson(int personId) {
        Gson json = new Gson();
        ArrayList<Integer> benefitId = getPersonBenefitsEdbo(personId);
        return json.toJson(benefitId);
    }

    /**
     * Синхронизация списков причин сдачи экзаменов вместо ЗНО
     */
    public void personRequestExaminationCauses() {
        if (personConnect() && mySqlConnect()) {
            ArrayOfDRequestExaminationCauses causesArray = personSoap.personRequestExaminationCausesGet(sessionGuid, languageId);
            List<DRequestExaminationCauses> causesList = causesArray.getDRequestExaminationCauses();
            for (DRequestExaminationCauses dCause : causesList) {
                String sql = "INSERT INTO `abiturient`.`causality`\n"
                        + "(`idCausality`,\n"
                        + "`CausalityName`,\n"
                        + "`CausalityDescription`)\n"
                        + "VALUES\n"
                        + "(\n"
                        + dCause.getIdPersonRequestExaminationCause() + ",\n"
                        + "'" + dCause.getPersonRequestExaminationCauseName().replaceAll("'", "\\\\'") + "',\n"
                        + "'" + dCause.getPersonRequestExaminationCauseDescription().replaceAll("'", "\\\\'") + "'\n"
                        + ");";
                try {
                    mySqlStatement.executeUpdate(sql);
                } catch (SQLException ex) {
                    Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * Добавить персону в базу ЕДБО
     *
     * @param personIdMySql Идентификатор персоны в базе MySql
     * @param entrantDocumentIdMySql Идентификатор документа об образовании в
     * базе MySQL
     * @param personalDocumentIdMySql Идентификатор документа об удостоверении
     * личности в базе MySQL
     * @return Статус попытки в формате json
     * @see edbosync.SubmitStatus
     */
    public String addPersonEdbo(int personIdMySql,
            int entrantDocumentIdMySql, int personalDocumentIdMySql) {

        SubmitStatus submitStatus = new SubmitStatus();
        String personCodeU = "";
        int personIdEdbo = -1;
        Gson json = new Gson();
        if (personConnect() && mySqlConnect()) {
            String sql = "SELECT * FROM person WHERE idPerson = " + personIdMySql + ";";
            try {
                ResultSet person = mySqlStatement.executeQuery(sql);
                if (person.next()) {
                    personCodeU = person.getString("codeU");
                    personIdEdbo = person.getInt("edboID");

                    if (personCodeU != null && (!personCodeU.isEmpty())) {
                        submitStatus.setError(false);
                        submitStatus.setGuid(personCodeU);
                        submitStatus.setId(personIdEdbo);
                        submitStatus.setBackTransaction(false);
                        submitStatus.setMessage("У персоны пристуствует код ЕДБО. Необходимость добавдения отсутствует.");
                        return json.toJson(submitStatus);
                    }
                    String birthday = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(person.getDate("Birthday"));
                    int personSexId = person.getInt("PersonSexID");
                    String firstName = person.getString("FirstName");
                    String middleName = person.getString("MiddleName");
                    String lastName = person.getString("LastName");
                    int resident = person.getInt("IsResident");
                    String adress = person.getString("Address");
                    String homeNumber = person.getString("HomeNumber");
                    String postIndex = person.getString("PostIndex");
                    int idStreetType = person.getInt("StreetTypeID");
                    int languageIdPerson = person.getInt("LanguageID");
                    String birthPlace = person.getString("BirthPlace");
                    int koatuuL1Id = person.getInt("KOATUUCodeL1ID");
                    int koatuuL2Id = person.getInt("KOATUUCodeL2ID");
                    int koatuuL3Id = person.getInt("KOATUUCodeL3ID");
                    int countryId = person.getInt("CountryID");
                    String koatuu = "";
                    ResultSet koatuuL3 = mySqlStatement.executeQuery(
                            "SELECT KOATUULevel3Code "
                            + "FROM koatuulevel3 "
                            + "WHERE "
                            + "idKOATUULevel3 = " + koatuuL3Id + ";");
                    if (koatuuL3.next()) {
                        koatuu = koatuuL3.getString(1);
                    } else {
                        ResultSet koatuuL2 = mySqlStatement.executeQuery(
                                "SELECT KOATUULevel2Code "
                                + "FROM koatuulevel2 "
                                + "WHERE "
                                + "idKOATUULevel2 = " + koatuuL2Id + ";");
                        if (koatuuL2.next()) {
                            koatuu = koatuuL2.getString(1);
                        } else {
                            ResultSet koatuuL1 = mySqlStatement.executeQuery(
                                    "SELECT KOATUULevel1Code "
                                    + "FROM koatuulevel1 "
                                    + "WHERE "
                                    + "idKOATUULevel1 = " + koatuuL1Id + ";");
                            if (koatuuL1.next()) {
                                koatuu = koatuuL1.getString(1);
                            } else {
                                Logger.getLogger(Synchronizer.class.getName()).log(Level.OFF, "KOATUU of Person ERROR!!!");
                            }
                        }
                    }

                    // данные документа об образовании
                    String sqlEntrantDocument = "SELECT * FROM abiturient.documents WHERE `documents`.`idDocuments` = " + entrantDocumentIdMySql + ";";
                    ResultSet entrantDocument = mySqlStatement.executeQuery(sqlEntrantDocument);
                    if (!entrantDocument.next()) {
                        Logger.getLogger(Synchronizer.class.getName()).log(Level.OFF, "Неверный идентификатор документа об образовании");
                        submitStatus.setMessage("Неверный идентификатор документа об образовании");
                        return json.toJson(submitStatus);
                    }
                    String entrantDocumentSeries = entrantDocument.getString("Series");
                    String entrantDocumentNumber = entrantDocument.getString("Numbers");
                    String entrantDocumentDate = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(entrantDocument.getDate("DateGet"));
                    String entrantDocumentValue = Float.toString(entrantDocument.getFloat("AtestatValue"));
                    String entrantDocumentIssued = entrantDocument.getString("Issued");
                    int entrantDocumentAwardTypeId = entrantDocument.getInt("PersonDocumentsAwardsTypesID");
                    int entrantDocumentTypeId = entrantDocument.getInt("TypeID");
                    int isForeigner = entrantDocument.getInt("isForeinghEntrantDocument");
                    int isNotCheckAttestat = entrantDocument.getInt("isNotCheckAttestat");

                    System.out.println(entrantDocumentValue);

                    // удостоверение личности
                    String sqlPersonalDocument = "SELECT * FROM abiturient.documents WHERE `documents`.`idDocuments` = " + personalDocumentIdMySql + ";";
                    ResultSet personalDocument = mySqlStatement.executeQuery(sqlPersonalDocument);
                    if (!personalDocument.next()) {
                        Logger.getLogger(Synchronizer.class.getName()).log(Level.OFF, "Неверный идентификатор документа об удостоверении личности");
                        submitStatus.setMessage("Неверный идентификатор документа об удостоверении личности");
                        return json.toJson(submitStatus);
                    }
                    String documentSeries = personalDocument.getString("Series");
                    String documentNumber = personalDocument.getString("Numbers");
                    String documentDate = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(personalDocument.getDate("DateGet"));
                    String documentIssued = personalDocument.getString("Issued");
                    int documentTypeId = personalDocument.getInt("TypeID");

//                    System.out.println(documentSeries + documentNumber + documentDate + documentIssued + documentTypeId);

                    String sqlLanguage = "SELECT * FROM abiturient.languages WHERE idLanguages = " + languageIdPerson + ";";
                    ResultSet languageResult = mySqlStatement.executeQuery(sqlLanguage);
                    if (!languageResult.next()) {
                        Logger.getLogger(Synchronizer.class.getName()).log(Level.OFF, "Неверный идентификатор иностранного языка персоны");
                        submitStatus.setMessage("Неверный идентификатор иностранного языка персоны");
                        return json.toJson(submitStatus);
                    }
                    String languages = languageResult.getString("LanguagesName");

                    String sqlContacts = "SELECT * FROM abiturient.personcontacts WHERE PersonID = " + personIdMySql + ";";
                    ResultSet contacts = mySqlStatement.executeQuery(sqlContacts);
                    String phone = "";
                    String mobile = "";
                    while (contacts.next()) {
                        if (contacts.getInt("PersonContactTypeID") == 1) {
                            phone = contacts.getString("Value");
                        }
                        if (contacts.getInt("PersonContactTypeID") == 2) {
                            mobile = contacts.getString("Value");
                        }
                    }
                    // загрузка персоны
                    ArrayOfDPersonAddRet personRetArray = personSoap.personEntrantAdd(
                            sessionGuid, // 1
                            languageId, // 2
                            resident, //3
                            birthday, //4
                            personSexId, // 5
                            firstName, // 6
                            middleName, // 7
                            lastName, // 8
                            koatuu, // 9
                            idStreetType, // 10
                            adress, // 11
                            homeNumber, // 12
                            entrantDocumentSeries, // 13
                            entrantDocumentNumber, // 14
                            entrantDocumentDate, // 15 
                            entrantDocumentValue, // 16
                            (documentTypeId == 3) ? documentSeries : "", // 17 паспорт
                            (documentTypeId == 3) ? documentNumber : "", // 18
                            (documentTypeId == 3) ? documentIssued : "", // 19
                            (documentTypeId == 3) ? documentDate : "", // 20
                            (documentTypeId == 1) ? documentSeries : "", // 21 свидетельство о рождении
                            (documentTypeId == 1) ? documentNumber : "", // 22
                            (documentTypeId == 1) ? documentDate : "", // 23
                            "", // 24
                            phone, // 25
                            mobile, // 26
                            "", // 27
                            "", // 28
                            "", // 29
                            isForeigner, // 30
                            isNotCheckAttestat, // 31
                            entrantDocumentTypeId, // 32
                            "", // 33
                            "", // 34
                            "", // 35
                            "", // 36
                            "", // 37
                            postIndex, // 38
                            birthPlace, // 39 место рождения
                            languages, // 40
                            entrantDocumentIssued, // 41
                            entrantDocumentAwardTypeId, // 42
                            1, // 43
                            (documentTypeId == 17) ? documentSeries : "", // 44
                            (documentTypeId == 17) ? documentNumber : "", // 45
                            (documentTypeId == 17) ? documentIssued : "", // 46
                            (documentTypeId == 17) ? documentDate : "", // 47
                            countryId); //804); // 48
                    if (personRetArray == null) {
                        submitStatus.setMessage(personSoap.getLastError(sessionGuid).getDLastError().get(0).getLastErrorDescription());
                        return json.toJson(submitStatus);
                    }
                    List<DPersonAddRet> personRetList = personRetArray.getDPersonAddRet();
                    for (DPersonAddRet personRet : personRetList) {
                        System.out.println(personRet.getPersonCodeU());
                        personCodeU = personRet.getPersonCodeU();
                        personIdEdbo = personRet.getIdPerson();
                    }
                    // Обновление кода и идентификатора персоны             
                    String sqlUpdatePersonCode = "UPDATE `abiturient`.`person`\n"
                            + "SET\n"
                            + "`codeU` = \"" + personCodeU + "\",\n"
                            + "`edboID` = " + personIdEdbo + "\n"
                            + "WHERE `idPerson` = " + personIdMySql + ";";
                    mySqlStatement.executeUpdate(sqlUpdatePersonCode);
                    // Обновление кодов документов
                    ArrayList<PersonDocument> personDocuments = getPersonDocumentEdbo(personCodeU);
                    for (PersonDocument document : personDocuments) {
                        if (entrantDocumentNumber.equalsIgnoreCase(document.getNumber()) && entrantDocumentSeries.equalsIgnoreCase(document.getSeries())) {
                            mySqlStatement.executeUpdate("UPDATE `abiturient`.`documents`\n"
                                    + "SET\n"
                                    + "`edboID` = " + document.getId_Document() + "\n"
                                    + "WHERE idDocuments = " + entrantDocumentIdMySql + ";");
                        }
                        if (documentNumber.equalsIgnoreCase(document.getNumber()) && documentSeries.equalsIgnoreCase(document.getSeries())) {
                            mySqlStatement.executeUpdate("UPDATE `abiturient`.`documents`\n"
                                    + "SET\n"
                                    + "`edboID` = " + document.getId_Document() + "\n"
                                    + "WHERE idDocuments = " + personalDocumentIdMySql + ";");
                        }
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        submitStatus.setError(false);
        submitStatus.setBackTransaction(false);
        submitStatus.setGuid(personCodeU);
        submitStatus.setId(personIdEdbo);
        return json.toJson(submitStatus);
    }

    /**
     * Добавить документы персоны в базу ЕДБО
     *
     * <p>Метод производит перебор документов в базе "Абитуриент". Для каждого
     * документа, у кторого отсутствует идентификатор записи в базе ЕДБО,
     * производится попытка добавления.</p>
     *
     * @param personIdMySql Идентификатор персоны в базе MySQL
     * @return Статус попытки в формате json
     * @see edbosync.SubmitStatus
     */
    public String addPersonDocumentsEdbo(int personIdMySql) {
        SubmitStatus submitStatus = new SubmitStatus();
        Gson json = new Gson();
        if (personConnect() && mySqlConnect()) {
            int edboIdPerson;
            try {
                // идентификатор персоны в базе ЕДБО
                ResultSet person = mySqlStatement.executeQuery("SELECT `person`.`edboID` FROM abiturient.person WHERE idPerson = " + personIdMySql + ";");
                person.next();
                edboIdPerson = person.getInt(1);
                if (person.wasNull()) {
                    submitStatus.setError(true);
                    submitStatus.setBackTransaction(false);
                    submitStatus.setMessage("Неможливо додати документи до персони, яка не пройшла синхронизацію з ЄДБО.");
                    return json.toJson(submitStatus);
                }

            } catch (SQLException ex) {
                Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
                submitStatus.setError(true);
                submitStatus.setBackTransaction(false);
                submitStatus.setMessage("Помилка з’єднання: " + ex.getLocalizedMessage());
                return json.toJson(submitStatus);
            }
            String sqlSelectDocuments = "SELECT * FROM abiturient.documents WHERE PersonID = " + personIdMySql + ";";
            ResultSet document;
            try {
                document = mySqlStatement.executeQuery(sqlSelectDocuments);
                while (document.next()) {
                    int idDocument = document.getInt("idDocuments");
                    int typeId = document.getInt("TypeID");
                    String series = document.getString("Series");
                    String number = document.getString("Numbers");
                    String dateGet = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(document.getDate("DateGet"));
                    int znoPin = (typeId == 4) ? document.getInt("ZNOPin") : 0;
                    String attestatValue = Float.toString(document.getFloat("AtestatValue"));
                    String issued = document.getString("Issued");
                    int awardTypeId = document.getInt("PersonDocumentsAwardsTypesID");
                    int edboId = document.getInt("edboID");
                    if (document.wasNull() || edboId == 0) {
                        edboId = personSoap.personDocumentsAdd(sessionGuid, 
                                languageId, 
                                edboIdPerson, 
                                typeId, 
                                0, 
                                (series != null) ? series : "", 
                                (number != null) ? number : "", 
                                (dateGet != null) ? dateGet : "", 
                                (issued != null) ? issued : "", 
                                "", 
                                znoPin, 
                                attestatValue, 
                                1, 
                                awardTypeId);
                        if (edboId == 0) {
                            submitStatus.setError(true);
                            submitStatus.setBackTransaction(false);
                            submitStatus.setMessage(submitStatus.getMessage() + personSoap.getLastError(sessionGuid).getDLastError().get(0).getLastErrorDescription());
                        }
                        document.updateInt("edboID", edboId);
                        document.updateRow();
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(Synchronizer.class.getName()).log(Level.SEVERE, null, ex);
                submitStatus.setError(true);
                submitStatus.setBackTransaction(false);
                submitStatus.setMessage("Помилка з’єднання: " + ex.getLocalizedMessage());
                return json.toJson(submitStatus);
            }

        } else {
            submitStatus.setError(true);
            submitStatus.setBackTransaction(false);
            submitStatus.setMessage("Помилка з’єднання.");
            return json.toJson(submitStatus);
        }
        submitStatus.setError(false);
        submitStatus.setBackTransaction(false);
        submitStatus.setMessage("Синхронизацію успіхно завершено.");
        return json.toJson(submitStatus);
    }

    public String getSoapUser() {
        return soapUser;
    }

    public String getSoapPassword() {
        return soapPassword;
    }

    public String getApplicationKey() {
        return applicationKey;
    }

    public String getSessionGuid() {
        return sessionGuid;
    }

    public String getActualDate() {
        return actualDate;
    }

    public int getLanguageId() {
        return languageId;
    }

    public int getSeasonId() {
        return seasonId;
    }

    public String getUniversityKey() {
        return universityKey;
    }

    public String getMySqlConnectionUrl() {
        return mySqlConnectionUrl;
    }

    public String getMySqlUser() {
        return mySqlUser;
    }

    public String getMySqlPassword() {
        return mySqlPassword;
    }

    public void setSoapUser(String soapUser) {
        this.soapUser = soapUser;
    }

    public void setSoapPassword(String soapPassword) {
        this.soapPassword = soapPassword;
    }

    public void setLanguageId(int languageId) {
        this.languageId = languageId;
    }

    public void setSeasonId(int seasonId) {
        this.seasonId = seasonId;
    }

    public void setMySqlUser(String mySqlUser) {
        this.mySqlUser = mySqlUser;
    }

    public void setMySqlPassword(String mySqlPassword) {
        this.mySqlPassword = mySqlPassword;
    }
}