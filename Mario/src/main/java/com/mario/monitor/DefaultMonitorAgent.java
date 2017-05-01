package com.mario.monitor;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.mario.contact.Contact;
import com.mario.monitor.config.MonitorAlertRecipientsConfig;
import com.mario.monitor.config.MonitorAlertServicesConfig;
import com.mario.monitor.config.MonitorAlertStatusConfig;
import com.mario.schedule.ScheduledCallback;
import com.mario.schedule.ScheduledFuture;
import com.mario.services.email.DefaultEmailEnvelope;
import com.mario.services.email.EmailService;
import com.mario.services.sms.SmsService;
import com.nhb.common.Loggable;

public class DefaultMonitorAgent extends BaseMonitorAgent implements Loggable {

	private ScheduledFuture monitorScheduledId;

	private ExecutorService executor = Executors.newCachedThreadPool();

	private Collection<Contact> extractContactListFromRecipientsConfig(MonitorAlertRecipientsConfig recipientsConfig) {
		Collection<Contact> results = new HashSet<>();
		for (String contactName : recipientsConfig.getContacts()) {
			Contact contact = getApi().getContactBook().getContact(contactName);
			if (contact != null) {
				results.add(contact);
			}
		}
		for (String group : recipientsConfig.getGroups()) {
			results.addAll(getApi().getContactBook().getContactByGroup(group));
		}
		return results;
	}

	@Override
	public void start() {
		getLogger().info("Start monitor agent {}", this.getName());
		this.monitorScheduledId = getApi().getScheduler().scheduleAtFixedRate(getInterval(), getInterval(),
				new ScheduledCallback() {

					@Override
					public void call() {
						MonitorableResponse response = getTarget().checkStatus();
						getLogger().debug("{} is execute check status on target", getName());
						if (response != null) {
							MonitorableStatus status = response.getStatus();
							MonitorAlertStatusConfig alertConfig = getAlertConfig().getStatusToConfigs().get(status);
							if (alertConfig != null) {
								Collection<Contact> contacts = extractContactListFromRecipientsConfig(
										alertConfig.getRecipientsConfig());
								if (contacts != null && contacts.size() > 0) {

									Collection<String> emails = new HashSet<>();
									Collection<String> phoneNumbers = new HashSet<>();

									for (Contact contact : contacts) {
										if (contact.getEmail() != null) {
											emails.add(contact.getEmail());
										}
										if (contact.getPhoneNumber() != null) {
											phoneNumbers.add(contact.getPhoneNumber());
										}
									}

									MonitorAlertServicesConfig servicesConfig = alertConfig.getServicesConfig();
									if (emails.size() > 0) {
										DefaultEmailEnvelope envelope = new DefaultEmailEnvelope();
										envelope.setContent(response.getMessage());
										envelope.setSubject("[" + getName() + " ALERT] " + status.name());
										envelope.getTo().addAll(emails);

										for (String emailServiceName : servicesConfig.getEmailServices()) {
											EmailService emailService = getApi().getEmailService(emailServiceName);
											if (emailService != null) {
												executor.submit(new Runnable() {

													@Override
													public void run() {
														try {
															emailService.send(envelope);
														} catch (Exception e) {
															getLogger().error("Error while sending email: ", e);
														}
													}
												});
											}
										}
									}

									if (phoneNumbers.size() > 0) {
										for (String smsServiceName : servicesConfig.getSmsServices()) {
											SmsService smsService = getApi().getSmsService(smsServiceName);
											if (smsService != null) {
												try {
													smsService.send(response.getMessage(), phoneNumbers);
												} catch (Exception e) {
													getLogger().error("Error while sending sms: ", e);
												}
											}
										}
									}
								}
							}
						}
					}
				});
	}

	@Override
	public void stop() {
		if (this.monitorScheduledId != null) {
			this.monitorScheduledId.cancel();
			this.monitorScheduledId = null;
		}
	}

}
