package cea.streamer.core;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class KDDClassificationRecord extends TimeRecord {
	
	private static String[] headers = {"duration",
	                          "src_bytes",
	                          "dst_bytes",
	                          "land",
	                          "wrong_fragment",
	                          "urgent",
	                          "hot",
	                          "num_failed_logins",
	                          "logged_in",
	                          "num_compromised",
	                          "root_shell",
	                          "su_attempted",
	                          "num_root",
	                          "num_file_creations",
	                          "num_shells",
	                          "num_access_files",
	                          "num_outbound_cmds",
	                          "is_host_login",
	                          "is_guest_login",
	                          "count",
	                          "srv_count",
	                          "serror_rate",
	                          "srv_serror_rate",
	                          "rerror_rate",
	                          "srv_rerror_rate",
	                          "same_srv_rate",
	                          "diff_srv_rate",
	                          "srv_diff_host_rate",
	                          "dst_host_count",
	                          "dst_host_srv_count",
	                          "dst_host_same_srv_rate",
	                          "dst_host_diff_srv_rate",
	                          "dst_host_same_src_port_rate",
	                          "dst_host_srv_diff_host_rate",
	                          "dst_host_serror_rate",
	                          "dst_host_srv_serror_rate",
	                          "dst_host_rerror_rate",
	                          "dst_host_srv_rerror_rate",
	                          "protocol_type_icmp",
	                          "protocol_type_tcp",
	                          "protocol_type_udp",
	                          "service_IRC",
	                          "service_X11",
	                          "service_Z39_50",
	                          "service_aol",
	                          "service_auth",
	                          "service_bgp",
	                          "service_courier",
	                          "service_csnet_ns",
	                          "service_ctf",
	                          "service_daytime",
	                          "service_discard",
	                          "service_domain",
	                          "service_domain_u",
	                          "service_echo",
	                          "service_eco_i",
	                          "service_ecr_i",
	                          "service_efs",
	                          "service_exec",
	                          "service_finger",
	                          "service_ftp",
	                          "service_ftp_data",
	                          "service_gopher",
	                          "service_harvest",
	                          "service_hostnames",
	                          "service_http",
	                          "service_http_2784",
	                          "service_http_443",
	                          "service_http_8001",
	                          "service_imap4",
	                          "service_iso_tsap",
	                          "service_klogin",
	                          "service_kshell",
	                          "service_ldap",
	                          "service_link",
	                          "service_login",
	                          "service_mtp",
	                          "service_name",
	                          "service_netbios_dgm",
	                          "service_netbios_ns",
	                          "service_netbios_ssn",
	                          "service_netstat",
	                          "service_nnsp",
	                          "service_nntp",
	                          "service_ntp_u",
	                          "service_other",
	                          "service_pm_dump",
	                          "service_pop_2",
	                          "service_pop_3",
	                          "service_printer",
	                          "service_private",
	                          "service_red_i",
	                          "service_remote_job",
	                          "service_rje",
	                          "service_shell",
	                          "service_smtp",
	                          "service_sql_net",
	                          "service_ssh",
	                          "service_sunrpc",
	                          "service_supdup",
	                          "service_systat",
	                          "service_telnet",
	                          "service_tftp_u",
	                          "service_tim_i",
	                          "service_time",
	                          "service_urh_i",
	                          "service_urp_i",
	                          "service_uucp",
	                          "service_uucp_path",
	                          "service_vmnet",
	                          "service_whois",
	                          "flag_OTH",
	                          "flag_REJ",
	                          "flag_RSTO",
	                          "flag_RSTOS0",
	                          "flag_RSTR",
	                          "flag_S0",
	                          "flag_S1",
	                          "flag_S2",
	                          "flag_S3",
	                          "flag_SF",
	                          "flag_SH",
	                          "label"};
	
	public KDDClassificationRecord() {
		super();
	}
	
	@Override
	public void fill(String data) {
		if(data.trim() != "") {
			String[] fields = data.trim().toLowerCase().split(";");
			if(fields.length > 1) {
				try {
					//This sleep is added for the reason that I am assigning the timestamps to the used dataset and thus we should have a little gap in time to obtain different microseconds
					TimeUnit.MICROSECONDS.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				source = fields[0];
				timestamp = new SimpleDateFormat("dd-MMM-yy HH:mm:ss.SSS", Locale.ENGLISH).format(new Date());
				// skip if this record contains the header of the csv file.
				if(fields[1].contains("duration")) {
					System.out.println(fields[1]);
					return;
				}
				String[] features = fields[1].split(",");
				//System.out.println(Arrays.toString(features));
				for(int i=0;i<features.length - 1;i++) {
					values.put(headers[i], features[i]);
					extractors.put(headers[i], new NumericalExtractor());
					//System.out.println(headers[i] + ": " + features[i]);
				}
				//String measurements = fields[1].substring(0, fields[1].lastIndexOf(","));
				String label = fields[1].substring(fields[1].lastIndexOf(",")+1);
				//values.put("meassure", measurements);
				//extractors.put("meassure", new NumericalExtractor());
				
				target = label;
				extractors.put("classification", new NumericalExtractor());
			}
			
		}
	}
	
	@Override
	public long getTimeStampMillis() {
		Calendar cal = Calendar.getInstance();	
		SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yy HH:mm:ss.SSS", Locale.ENGLISH);
		Date dateTime;
		try {
			dateTime = format.parse(timestamp);
			cal.setTime(dateTime);
		} catch (ParseException e) {
			e.printStackTrace();
		}			
		return cal.getTimeInMillis();
	}
}
