package com.mgs.testtask.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.*;

@Controller
public class GeneratorController
{
	@Value("#{new Integer('${generator.masslength.default}')}")
	private Integer massLength;

	@Value("#{new Boolean('${generator.isauto.default}')}")
	private Boolean isAuto;

	@Value("#{new Integer('${generator.sequencelength}')}")
	private Integer sequenceLength;

	@Value("#{new Integer('${generator.sequencecount}')}")
	private Integer sequenceCount;

	private Set<Integer> mass = new HashSet<>();

	private SimpMessagingTemplate template;

	public GeneratorController(SimpMessagingTemplate template)
	{
		this.template = template;
	}

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String getGeneratorView()
	{
		return "generator";
	}

	@MessageMapping("/generator.mass")
	@SendTo("/topic/mass")
	public Set<Integer> generateMass()
	{
		mass.clear();
		int current = 1;
		while (mass.size() < massLength)
		{
			if (isPrime(++current))
				mass.add(current);
		}
		return mass;
	}

	public static Boolean isPrime(Integer number)
	{
		for (int i = 2; i < (number / 2) + 1; ++i)
			if (number % i == 0)
				return false;
		return true;
	}

	@MessageMapping("/generator.auto")
	@SendTo("/topic/auto")
	public Boolean switchAuto(@Payload Boolean isAutoValue)
	{
		isAuto = isAutoValue;
		return isAuto;
	}

	@SubscribeMapping("/generator.settings")
	public Map<String, String> getCurrentAuto()
	{
		Map<String, String> settings = new HashMap<>();
		settings.put("massLength", massLength.toString());
		settings.put("isAuto", isAuto.toString());
		return settings;
	}

	@MessageMapping("/generator.masslength")
	@SendTo("/topic/length")
	public Integer generatorLength(@Payload Integer generatorLengthValue)
	{
		massLength = generatorLengthValue;
		return massLength;
	}

	@MessageMapping("/generator.sequence")
	public void generate()
	{
		for (int i = 0; i < sequenceCount; ++i)
			template.convertAndSend("/topic/sequence", getNewSequence());
	}

	private Set<Integer> getNewSequence()
	{
		Random random = new Random();
		Set<Integer> sequence = new HashSet<>();
		Integer[] numbersArray = mass.toArray(new Integer[mass.size()]);
		while (sequence.size() < sequenceLength)
			sequence.add(numbersArray[random.nextInt(numbersArray.length)]);
		return sequence;
	}

	@Scheduled(cron = "${generator.isauto.schedule}")
	public void autoGenerate()
	{
		if (isAuto)
			template.convertAndSend("/topic/sequence", getNewSequence());
	}
}
