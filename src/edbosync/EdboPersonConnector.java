package edbosync;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import ua.edboservice.ArrayOfDLastError;
import ua.edboservice.DLastError;
import ua.edboservice.EDBOPerson;
import ua.edboservice.EDBOPersonSoap;

/**
 * Класс для соединения с сервером ЕДБО (персоны)
 *
 * @author Сергей Чопоров
 */
public class EdboPersonConnector {

    protected EDBOPerson edboPerson = new EDBOPerson();
    /**
     * Экземпляр Soap-потока персоны
     */
    protected EDBOPersonSoap soap = null;
    /**
     * Идентификатор соап-сессии
     */
    protected String sessionGuid = "";
    /**
     * Текущая дата
     */
    protected String actualDate = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(java.util.Calendar.getInstance().getTime());
    /**
     * Идентификатор языка (1 - укр)
     */
    protected int languageId = 1;
    /**
     * Идентификатор вступительной компании (2 - 2012, 3 - 2013, ...)
     */
    protected int seasonId = 6;
    /**
     * Ключ университета в ЕДБО
     */
    String universityKey = "ab1bc732-51f3-475c-bcfe-368363369020";

    /**
     * Конструктор по умолчанию создает соединение со службой EDBO Person
     */
    public EdboPersonConnector() {
        login();
    }

    /**
     * Сборщик мусора закрывает соединение с ЕДБО при уничтожении экземпляра
     * класса
     */
    @Override
    protected void finalize() {
        try {
            logout();
        } finally {
            try {
                super.finalize();
            } catch (Throwable ex) {
                Logger.getLogger(EdboPersonConnector.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Установка соединения со службой EDBO Person
     *
     * @return true, если соединение установлено, false - иначе
     */
    protected final boolean login() {
        // wsdl connection url:
        // http://edbo.znu.edu.ua:8080/EDBOPerson/EDBOPerson.asmx?WSDL
        // http://iskt-1.znu.edu.ua:9091/EDBOPerson/EDBOPerson.asmx?WSDL
        SoapConnectionData data = new SoapConnectionData();
        soap = edboPerson.getEDBOPersonSoap();
        sessionGuid = soap.login(data.getSoapUser(), data.getSoapPassword(), 0, data.getApplicationKey());
//        sessionGuid = soap.login("davidovskij.v@edbo.gov.ua", "testpass1917", 0, ""); //// TEST !!!!!!!!!!!!!!!!
        if (sessionGuid.length() != 36) {
            // при соединении возникла ошибка
            System.err.println(sessionGuid);
            return false;
        }
        return true;
    }

    /**
     * Закрытие соединения со службой EDBO Person
     *
     * @return true, если соединение установлено, false - иначе
     */
    protected final boolean logout() {
        String result = soap.logout(sessionGuid);
        if (result.length() > 0) {
            System.err.println(result);
            return false;
        }
        return true;
    }

    /**
     * Обработчик ошибок, которые возвращает сервер ЕДБО
     *
     * @return Строка с сообщением об ошибках
     */
    public String processErrors() {
        String[] smiles = {
            ":-)", ":lol:", ":cheese:", ":)", ";-)", ";)", ":-S", ":wow:", ":bug:", ":-P", "%-P", ";-P", ":P", "8-/", ":-/", ">:(", ">:-(", ":-O"
        };
        String[] images = {
            "_/﹋\\_\n" +
"(҂`_´)\n" +
"<,︻╦╤─ ҉ - - - - - - --\n" +
"_/﹋\\_",
            "░░░░██▄\n" +
"░░░██▀░░░░▐\n" +
"▌░███▄░░░░▐\n" +
"▌▐███░▀▄███▄▄▄██▄▄\n" +
"▌█████▌░░▌░░░░░░▌\n" +
"▌▀▀▀▌▐█░░▌░░░░░░▌\n" +
"▌▀▀▀▌▐█░░▌░░░░░░▌\n" +
"▌░░░▌░█▄▌░░░░░░░▌",
            "¦•¦♥¦╔═╦╦╦═╦═╦═╗ ¦•¦♥¦\n" +
"¦•¦♥¦║═╣║║╬║═╣╬║ ¦•¦♥¦\n" +
"¦•¦♥¦╠═║║║╔╣═╣╔╣ ¦•¦♥¦\n" +
"¦•¦♥¦╚═╩═╩╝╚═╩╩╝ ¦•¦♥¦",
            "╔┓┏╦━━╦┓╔┓╔━━╗\n" +
"║┗┛║┗━╣┃║┃║╯╰║\n" +
"║┏┓║┏━╣┗╣┗╣╰╯║\n" +
"╚┛┗╩━━╩━╩━╩━━╝",
            "██████　██████\n" +
"███▄▄█　█▀▀███\n" +
"█████▄　▀█████\n" +
"████▀▀　██████\n" +
"██████　▄▄████\n" +
"██████　██████",
            "████▐▐▐██████████\n" +
"███▐▐▐▐███▌▌▌████\n" +
"███───▐▌██▌▌▌▌███\n" +
"███▄──▄▄█▐▌───███\n" +
"█████████▄▄──▄███",
            ". . . . /\\„.„/\\\n" +
". . . .(=’♥’=). . . . . . ./\\„„/\\\n" +
". . ../ ‾`•´‾ \\. . . . . .(=’•’=)\n" +
". . . \\ .\\ …/. /. . . . . ./ . . . \\\n" +
". . ..(,,)⌣ (,,)__)……...\\„/⌣\\„/__)\n" +
" ”´”`´”\"`”`´”´”`´”\"”´”`´”\"´”´ МЯУ!!!",
            "▄██▄██▄\n" +
"▀██♥██▀\n" +
"░░▀█▀\n" +
"░░░❤▄██▄██▄\n" +
"░░░ ♥▀██♥██▀\n" +
"░░░░░ ♥▀█▀\n" +
"▄██▄██▄\n" +
"▀██♥██▀\n" +
"░░▀█▀\n" +
"░░░❤\n" +
"░░░♥\n" +
"░░♥"
        };
        ArrayOfDLastError errorArray;
        errorArray = soap.getLastError(sessionGuid);
        String finalMessage = "";
        List<DLastError> errorList = errorArray.getDLastError();
        for (DLastError dError : errorList) {
            int iSmile = (int)(Math.random() * smiles.length);
            int iImage = (int)(Math.random() * images.length);
            System.err.println(dError.getLastErrorDescription());
            if (dError.getLastErrorDescription() == null || dError.getLastErrorDescription().isEmpty())
                finalMessage = finalMessage + "Ooops.<br />" + smiles[iSmile] + "<br />" + "Network Error!<br />" + images[iImage] + "<br />";
            else
                finalMessage = finalMessage + dError.getLastErrorDescription() + " ";
        }
        return finalMessage;
    }

    /**
     * Обработчик ошибок, которые возвращает сервер ЕДБО
     *
     * @return Строка с сообщением об ошибках в формате json
     */
    public String processErrorsJson() {
        return "{\"error\":\"" + processErrors() + "\"}";
    }

    public EDBOPersonSoap getSoap() {
        return soap;
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

    public void setLanguageId(int languageId) {
        this.languageId = languageId;
    }

    public void setSeasonId(int seasonId) {
        this.seasonId = seasonId;
    }

}
