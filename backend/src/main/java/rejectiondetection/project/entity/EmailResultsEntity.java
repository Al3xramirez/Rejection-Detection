package rejectiondetection.project.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_results")
public class EmailResultsEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "company_name", length = 150)
	private String companyName;

	@Column(name = "email_subject", length = 255)
	private String emailSubject;

	@Column(name = "email_sender", length = 255)
	private String emailSender;

	@Column(name = "status", nullable = false, length = 50)
	private String status;

	@Column(name = "matched_phrase", length = 255)
	private String matchedPhrase;

	@Column(name = "summary", columnDefinition = "TEXT")
	private String summary;

	@Column(name = "funny_message", length = 255)
	private String funnyMessage;

	@Column(name = "received_at")
	private LocalDateTime receivedAt;

	@Column(name = "scanned_at")
	private LocalDateTime scannedAt;
	
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	public String getEmailSubject() {
		return emailSubject;
	}

	public void setEmailSubject(String emailSubject) {
		this.emailSubject = emailSubject;
	}

	public String getEmailSender() {
		return emailSender;
	}

	public void setEmailSender(String emailSender) {
		this.emailSender = emailSender;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getMatchedPhrase() {
		return matchedPhrase;
	}

	public void setMatchedPhrase(String matchedPhrase) {
		this.matchedPhrase = matchedPhrase;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getFunnyMessage() {
		return funnyMessage;
	}

	public void setFunnyMessage(String funnyMessage) {
		this.funnyMessage = funnyMessage;
	}

	public LocalDateTime getReceivedAt() {
		return receivedAt;
	}

	public void setReceivedAt(LocalDateTime receivedAt) {
		this.receivedAt = receivedAt;
	}

	public LocalDateTime getScannedAt() {
		return scannedAt;
	}

	public void setScannedAt(LocalDateTime scannedAt) {
		this.scannedAt = scannedAt;
	}
	
	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
	
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}