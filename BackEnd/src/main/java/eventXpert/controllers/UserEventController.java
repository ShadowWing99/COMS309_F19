package eventXpert.controllers;

import eventXpert.entities.*;
import eventXpert.enums.PointTypes;
import eventXpert.errors.*;
import eventXpert.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static eventXpert.enums.PointTypes.*;

/**
 * This is a controller for urls that begin with /userEvents
 */
@RestController
@RequestMapping(path = "/userEvents")
public class UserEventController {
	/**
	 * A UserEventRepository that is autogenerated by Spring.
	 * userEventRepository can be used to make CRUD operations to the database
	 */
	@Autowired
	private UserEventRepository userEventRepository;
	/**
	 * A UserRepository autogenerated by Spring.
	 */
	@Autowired
	private UserRepository userRepository;
	/**
	 * An EventRepository autogenerated by Spring.
	 */
	@Autowired
	private EventRepository eventRepository;
	@Autowired
	private MasteryRepository masteryRepository;
	
	/**
	 * Gets all userEvent relationships in database
	 *
	 * @return list of all userEvents
	 */
	@GetMapping(path = "")
	public @ResponseBody
	Iterable<UserEvent> getAllUserEvents() {
		return userEventRepository.findAll();
	}
	
	/**
	 * Adds a user event relationship to the database
	 *
	 * @param userId  id of user
	 * @param eventId id of event
	 * @return new userEvent that was created
	 */
	@PostMapping(path = "/users/{userId}/events/{eventId}/{isAdmin}")
	public @ResponseBody
	UserEvent addUserEvent(
			@PathVariable("userId") Integer userId,
			@PathVariable("eventId") Integer eventId,
			@PathVariable("isAdmin") boolean isAdmin) {
		checkParams(userId, eventId);
		
		UserEvent userEvent = new UserEvent(userId, eventId);
		userEvent.setIsAdmin(isAdmin);
		
		if (isAdmin) {
			addPointsToUser(userId, CREATE);
		}
		
		addPointsToUser(userId, REGISTER);
		
		return userEventRepository.save(userEvent);
	}
	
	/**
	 * Finds all of the userEvent relationships associated with a given userId and eventId.
	 *
	 * @param userId  id of a user
	 * @param eventId id of an event
	 * @return
	 */
	@GetMapping(path = "/users/{userId}/events/{eventId}")
	public @ResponseBody
	UserEvent getUserEvent(@PathVariable("userId") Integer userId, @PathVariable("eventId") Integer eventId) {
		checkParams(userId, eventId);
		Integer userEventId = userEventRepository.findIdByUserAndEvent(userId, eventId);
		return userEventRepository.findById(userEventId).get();
	}
	
	/**
	 * Gets the events associated with a giver user ID
	 *
	 * @param userId id of a user
	 * @return Iterable of a given user's Events
	 */
	@GetMapping(path = "/users/{userId}/events")
	public @ResponseBody
	Iterable<Event> getEventsByUser(@PathVariable("userId") Integer userId) {
		if (!userRepository.existsById(userId)) {
			throw new ResourceNotFoundException("User not found by id");
		}
		Iterable<Integer> eventIds = userEventRepository.findEventsByUserId(userId);
		Iterator<Integer> iterIds = eventIds.iterator();
		List<Event> events = new ArrayList<>();
		while (iterIds.hasNext()) {
			events.add((eventRepository.findById(iterIds.next()).get()));
		}
		Iterable<Event> userEvents = events;
		return userEvents;
	}
	
	/**
	 * Gets the users associated with a given eventId
	 *
	 * @param eventId id of an event
	 * @return Iterable of user's for a given event
	 */
	@GetMapping(path = "/events/{eventId}/users")
	public @ResponseBody
	Iterable<User> getUsersByEvent(@PathVariable("eventId") Integer eventId) {
		if (!eventRepository.existsById(eventId)) {
			throw new ResourceNotFoundException("Event not found by Id");
		}
		
		Iterable<Integer> userIds = userEventRepository.findUsersByEventId(eventId);
		Iterator<Integer> iterIds = userIds.iterator();
		List<User> users = new ArrayList();
		while (iterIds.hasNext()) {
			users.add(userRepository.findById(iterIds.next()).get());
		}
		return users;
	}
	
	/**
	 * Saves modified user event data
	 *
	 * @param modifiedUserEvent
	 * @return updated user event
	 */
	@PutMapping(path = "/{userEventId}")
	public UserEvent saveUserEvent(@RequestBody UserEvent modifiedUserEvent, @PathVariable("userEventId") Integer userEventId) {
		UserEvent ue = userEventRepository.findById(userEventId).get();
		checkParams(ue.getUserId(), ue.getEventId());
		
		ue.setIsAdmin(modifiedUserEvent.getIsAdmin());
		ue.setCheckedIn(modifiedUserEvent.getIsCheckedIn());
		
		if (modifiedUserEvent.getIsCheckedIn()) {
			addPointsToUser(modifiedUserEvent.getUserId(), CHECK_IN);
		}
		
		return userEventRepository.save(ue);
	}
	
	private void addPointsToUser(Integer userId, PointTypes pointType) {
		User user = userRepository.findById(userId).get();
		Mastery mastery = ((List<Mastery>) masteryRepository.findMasteryByPoints(user.getPoints())).get(0);
		int pointsToAdd = 0;
		
		switch (pointType) {
			case CHECK_IN:
				pointsToAdd = mastery.getCheckInPoints();
				break;
			case CREATE:
				pointsToAdd = mastery.getCreateEventPoints();
				break;
			case REGISTER:
				pointsToAdd = mastery.getRegisterPoints();
				break;
		}
		user.setPoints(user.getPoints() + pointsToAdd);
		userRepository.save(user);
	}
	
	/**
	 * Validates userId and eventId exist.
	 *
	 * @param userId  id of a user
	 * @param eventId id of an event
	 */
	private void checkParams(Integer userId, Integer eventId) {
		if (userId == null) {
			throw new BadRequestException("User id is null");
		}
		if (eventId == null) {
			throw new BadRequestException("Event id is null");
		}
		if (!userRepository.existsById(userId)) {
			throw new ResourceNotFoundException("User not found by id");
		}
		if (!eventRepository.existsById(eventId)) {
			throw new ResourceNotFoundException("Event not found by id");
		}
	}
}