package org.openkoala.organisation.facade.impl.assembler;

import org.openkoala.organisation.domain.Job;
import org.openkoala.organisation.facade.dto.JobDTO;

public class JobDtoAssembler {

	public static JobDTO assemDto(Job job) {
		JobDTO dto = new JobDTO(job.getId(), job.getName());
		dto.setSn(job.getSn());
		dto.setCreateDate(job.getCreateDate());
		dto.setDescription(job.getDescription());
		dto.setVersion(job.getVersion());
		return dto;
	}

	public static Job assemEntity(JobDTO jobDTO) {
		Job result = new Job(jobDTO.getName(), jobDTO.getSn());
		result.setId(jobDTO.getId());
		result.setDescription(jobDTO.getDescription());
		result.setVersion(jobDTO.getVersion());
		
		if (jobDTO.getCreateDate() != null) {
			result.setCreateDate(jobDTO.getCreateDate());
		}
		if (jobDTO.getTerminateDate() != null) {
			result.setTerminateDate(jobDTO.getTerminateDate());
		}
		
		return result;
	}

}