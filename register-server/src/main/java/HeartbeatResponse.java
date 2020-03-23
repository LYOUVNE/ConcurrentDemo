import lombok.Data;

/**
 * 心跳响应
 *
 */
@Data
public class HeartbeatResponse {
	
	public static final String SUCCESS = "success";
	public static final String FAILURE = "failure";

	/**
	 * 心跳响应状态：SUCCESS、FAILURE
	 */
	private String status;
}
